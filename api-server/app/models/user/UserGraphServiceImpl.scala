package models.user

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.muguang.core.db.MongoHelper
import com.muguang.core.exceptions.ResourceNotFoundException
import com.muguang.util.RandomUtils
import models.{ UserSummary, User }
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.api.QueryOpts
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes.{ Index, IndexType }
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import module.sihouette.WeiboProfile
import reactivemongo.core.commands.Count
import services.UserGraphService

import scala.concurrent.Future

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserGraphServiceImpl @Inject() (userDAO: UserDAO) extends UserGraphService with MongoHelper {

  val followingCollection = db.collection[BSONCollection]("following")
  val followersCollection = db.collection[BSONCollection]("followers")
  val blockChatCollection = db.collection[BSONCollection]("blockchat")
  val blacklistCollection = db.collection[BSONCollection]("blacklist")

  override def ensureIndexes: Future[List[Boolean]] = {
    for {
      followingIndex <- ensureIndex(followingCollection, List(("_f", IndexType.Ascending), ("_t", IndexType.Ascending)), unique = true)
      followersIndex <- ensureIndex(followersCollection, List(("_f", IndexType.Ascending), ("_t", IndexType.Ascending)), unique = true)
      blockChatIndex <- ensureIndex(blockChatCollection, List(("_f", IndexType.Ascending), ("_t", IndexType.Ascending)), unique = true)
      blacklistIndex <- ensureIndex(blacklistCollection, List(("_f", IndexType.Ascending), ("_t", IndexType.Ascending)), unique = true)
    } yield {
      List(followingIndex.inError, followersIndex.inError, blockChatIndex.inError, blacklistIndex.inError)
    }
  }

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userDAO.find(loginInfo)

  override def retrieve(userId: BSONObjectID): Future[Option[User]] = userDAO.findById(userId)

  override def save(user: User) = userDAO.save(user)

  override def save(profile: CommonSocialProfile) = {
    userDAO.find(profile.loginInfo).flatMap {
      case Some(user) =>
        // Not update profile if find user, but update refresh token
        userDAO.update(user.identify, user.copy(refreshToken = Some(RandomUtils.generateToken()))).map {
          case Left(ex) => throw ex
          case Right(u) => u
        }
      case None => // Insert a new user
        userDAO.save(User(
          _id = BSONObjectID.generate,
          loginInfo = profile.loginInfo,
          refreshToken = Some(RandomUtils.generateToken()),
          screenName = profile.firstName.getOrElse("") + " " + profile.lastName.getOrElse(""),
          email = profile.email,
          avatarUrl = profile.avatarURL
        ))
    }
  }

  override def save(profile: WeiboProfile): Future[User] = {
    userDAO.find(profile.loginInfo).flatMap {
      case Some(user) =>
        // Not update profile if find user, but update refresh token
        userDAO.update(user.identify, user.copy(refreshToken = Some(RandomUtils.generateToken()))).map {
          case Left(ex) => throw ex
          case Right(u) => u
        }
      case None => // Insert a new user
        userDAO.save(User(
          _id = BSONObjectID.generate,
          loginInfo = profile.loginInfo,
          refreshToken = Some(RandomUtils.generateToken()),
          screenName = profile.screenName,
          email = profile.email,
          biography = profile.biography,
          location = profile.location,
          avatarUrl = profile.avatarUrl,
          gender = profile.gender
        ))
    }
  }

  override def validateUser(userId: String): Future[User] = {
    try {
      userDAO.findById(userId).map {
        case Some(user) => user
        case None => throw ResourceNotFoundException(userId)
      }
    } catch {
      case e: Throwable => throw ResourceNotFoundException(userId)
    }

  }

  override def follow(from: BSONObjectID, to: BSONObjectID): Future[Unit] = {
    // Use the some edge _id for both edge collections
    val edgeId = BSONObjectID.generate

    for {
      // create the "following" relationship
      following <- UnsafeRecover(followingCollection.insert(BSONDocument("_id" -> edgeId, "_f" -> from, "_t" -> to))) {}
      // create the reverse "follower" relationship
      followers <- UnsafeRecover(followersCollection.insert(BSONDocument("_id" -> edgeId, "_f" -> to, "_t" -> from))) {}
      // update the following and follower counts of the two users respectively
      followingCount <- HandleDBFailure(userDAO.update(from, Json.obj("$inc" -> Json.obj("_cfg" -> 1))))
      followersCount <- HandleDBFailure(userDAO.update(to, Json.obj("$inc" -> Json.obj("_cfr" -> 1))))
    } yield {}
  }

  override def unfollow(from: BSONObjectID, to: BSONObjectID): Future[Unit] = {
    for {
      following <- UnsafeRecover(followingCollection.remove(BSONDocument("_f" -> from, "_t" -> to))) {}
      followers <- UnsafeRecover(followersCollection.remove(BSONDocument("_f" -> to, "_t" -> from))) {}
      // update the following and follower counts of the two users respectively
      followingCount <- HandleDBFailure(userDAO.update(from, Json.obj("$inc" -> Json.obj("_cfg" -> -1))))
      followersCount <- HandleDBFailure(userDAO.update(to, Json.obj("$inc" -> Json.obj("_cfr" -> -1))))
    } yield {}
  }

  override def getFollowersCount(userId: BSONObjectID): Future[Int] = {
    followersCollection.db.command(Count("followers", Some(BSONDocument("_f" -> userId))))
  }

  override def getFollowers(userId: BSONObjectID, skip: Int = 0, limit: Int = Int.MaxValue): Future[List[BSONObjectID]] = {
    followersCollection
      .find(BSONDocument("_f" -> userId), BSONDocument("_t" -> 1))
      .options(QueryOpts(skipN = skip))
      .cursor[BSONDocument]
      .collect[List](limit)
      .map(list => list.flatMap(_.getAs[BSONObjectID]("_t")))
  }

  override def getFollowingCount(userId: BSONObjectID): Future[Int] = {
    followersCollection.db.command(Count("following", Some(BSONDocument("_f" -> userId))))
  }

  override def getFollowing(userId: BSONObjectID, skip: Int = 0, limit: Int = Int.MaxValue): Future[List[BSONObjectID]] = {
    followingCollection
      .find(BSONDocument("_f" -> userId), BSONDocument("_t" -> 1))
      .options(QueryOpts(skipN = skip))
      .cursor[BSONDocument]
      .collect[List](limit)
      .map(list => list.flatMap(_.getAs[BSONObjectID]("_t")))
  }

  override def isFollowing(from: BSONObjectID, to: BSONObjectID): Future[Int] = {
    for {
      isfollowing <- followingCollection.find(BSONDocument("_f" -> from, "_t" -> to)).one[BSONDocument]
      isfollowed <- followersCollection.find(BSONDocument("_f" -> to, "_t" -> from)).one[BSONDocument]
    } yield {
      (isfollowing, isfollowed) match {
        case (Some(fg), Some(fr)) => 2
        case (Some(fg), None) => 1
        case (None, None) => 0
        case (None, Some(fr)) => -1
      }
    }
  }

  override def getUserSummary(userId: BSONObjectID): Future[UserSummary] = {
    validateUser(userId.stringify).map { user =>
      UserSummary(None,
        user.screenName,
        user.avatarUrl,
        user.biography,
        user.location,
        Some(user.postCount),
        Some(user.followingCount),
        Some(user.followersCount))
    }
  }

  override def getUerSummaryByIds(ids: List[BSONObjectID]): Future[List[UserSummary]] = {
    for {
      followers <- userDAO.getUserByIds(ids)
    } yield {
      followers.map(u => UserSummary(Some(u._id.stringify), u.screenName, u.avatarUrl))
    }
  }

  override def getRefreshTokenByUserId(userId: String): Future[Option[(Option[String], LoginInfo)]] = {
    userDAO.findById(userId).map { userOpt =>
      userOpt.map(user => (user.refreshToken, user.loginInfo))
    }
  }

  override def blockChat(from: BSONObjectID, to: BSONObjectID): Future[Unit] = {
    // Use the some edge _id for both edge collections
    val edgeId = BSONObjectID.generate
    UnsafeRecover(blockChatCollection.insert(BSONDocument("_id" -> edgeId, "_f" -> from, "_t" -> to))) {}
  }

  override def unblockChat(from: BSONObjectID, to: BSONObjectID): Future[Unit] = {
    UnsafeRecover(blockChatCollection.remove(BSONDocument("_f" -> from, "_t" -> to))) {}
  }

  override def blacklist(from: BSONObjectID, to: BSONObjectID): Future[Unit] = {
    // Use the some edge _id for both edge collections
    val edgeId = BSONObjectID.generate
    for {
      black <- UnsafeRecover(blacklistCollection.insert(BSONDocument("_id" -> edgeId, "_f" -> from, "_t" -> to))) {}
      block <- blockChat(from, to)
      unfollow1 <- unfollow(from, to)
      unfollow2 <- unfollow(to, from)
    } yield {}
  }

  override def isBlocked(from: BSONObjectID, to: BSONObjectID): Future[Boolean] = {
    blockChatCollection.find(BSONDocument("_f" -> to, "_t" -> from)).one[BSONDocument].map(_.isDefined)
  }

  override def isInBlacklist(from: BSONObjectID, to: BSONObjectID): Future[Boolean] = {
    blacklistCollection.find(BSONDocument("_f" -> to, "_t" -> from)).one[BSONDocument].map(_.isDefined)
  }

  private def ensureIndex(collection: BSONCollection,
    key: List[(String, IndexType)],
    name: Option[String] = None,
    unique: Boolean = false,
    background: Boolean = false,
    dropDups: Boolean = false,
    sparse: Boolean = false,
    version: Option[Int] = None,
    options: BSONDocument = BSONDocument()) = {
    val index = Index(key, name, unique, background, dropDups, sparse, version, options)
    Logger.info(s"Collection[${collection.name}] ensuring index: $index")
    collection.indexesManager.create(index)
  }
}
