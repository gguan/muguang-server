package models.user

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.muguang.core.db.MongoHelper
import com.muguang.core.exceptions.ResourceNotFoundException
import com.muguang.util.RandomUtils
import models.{ RefreshToken, UserSummary, User }
import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.api.QueryOpts
import reactivemongo.api.collections.default.BSONCollection
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

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userDAO.find(loginInfo)

  override def retrieve(userId: BSONObjectID): Future[Option[User]] = userDAO.findById(userId)

  override def save(user: User) = userDAO.save(user)

  override def save(profile: CommonSocialProfile) = {
    userDAO.find(profile.loginInfo).flatMap {
      case Some(user) =>
        // Not update profile if find user, but update refresh token
        userDAO.update(user.identify, user.copy(refreshToken = Some(RefreshToken(RandomUtils.generateToken(), DateTime.now().plusDays(30)))))
          .map(result => result match {
            case Left(ex) => throw ex
            case Right(u) => u
          })
      case None => // Insert a new user
        userDAO.save(User(
          _id = BSONObjectID.generate,
          loginInfo = profile.loginInfo,
          refreshToken = Some(RefreshToken(RandomUtils.generateToken(), DateTime.now().plusDays(30))),
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
        userDAO.update(user.identify, user.copy(refreshToken = Some(RefreshToken(RandomUtils.generateToken(), DateTime.now().plusDays(30)))))
          .map(result => result match {
            case Left(ex) => throw ex
            case Right(u) => u
          })
      case None => // Insert a new user
        userDAO.save(User(
          _id = BSONObjectID.generate,
          loginInfo = profile.loginInfo,
          refreshToken = Some(RefreshToken(RandomUtils.generateToken(), DateTime.now().plusDays(30))),
          screenName = profile.screenName,
          email = profile.email,
          biography = profile.biography,
          location = profile.location,
          avatarUrl = profile.avatarUrl,
          gender = profile.gender
        ))
    }
  }

  override def validateUser(userId: BSONObjectID): Future[User] = {
    try {
      userDAO.findById(userId).map(userOpt => userOpt match {
        case Some(user) => user
        case None => throw ResourceNotFoundException(userId.stringify)
      })
    } catch {
      case e: Throwable => throw ResourceNotFoundException(userId.stringify)
    }

  }

  override def follow(from: BSONObjectID, to: BSONObjectID): Future[Unit] = {

    // Use the some edge _id for both edge collections
    val edgeId = BSONObjectID.generate

    for {
      // create the "following" relationship
      following <- Recover(followingCollection.insert(BSONDocument("_id" -> edgeId, "_f" -> from, "_t" -> to))) {}
      // create the reverse "follower" relationship
      followers <- Recover(followersCollection.insert(BSONDocument("_id" -> edgeId, "_f" -> to, "_t" -> from))) {}
      // update the following and follower counts of the two users respectively
      followingCount <- userDAO.update(from, Json.obj("$inc" -> Json.obj("_cfg" -> 1)))
      followersCount <- userDAO.update(to, Json.obj("$inc" -> Json.obj("_cfr" -> 1)))
    } yield {}
  }

  override def unfollow(from: BSONObjectID, to: BSONObjectID): Future[Unit] = {

    for {
      following <- Recover(followingCollection.remove(BSONDocument("_f" -> from, "_t" -> to))) {}
      followers <- Recover(followersCollection.remove(BSONDocument("_f" -> to, "_t" -> from))) {}
      // update the following and follower counts of the two users respectively
      followingCount <- userDAO.update(from, Json.obj("$inc" -> Json.obj("_cfg" -> -1)))
      followersCount <- userDAO.update(to, Json.obj("$inc" -> Json.obj("_cfr" -> -1)))
    } yield {}
  }

  override def getFollowersCount(userId: BSONObjectID): Future[Int] = {
    followersCollection.db.command(
      Count(
        "followers",
        Some(BSONDocument("_f" -> userId))
      )
    )
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
    followersCollection.db.command(
      Count(
        "following",
        Some(BSONDocument("_f" -> userId))
      )
    )
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
    validateUser(userId).map { user =>
      UserSummary(None,
        user.screenName,
        user.avatarUrl,
        user.biography,
        user.location,
        Some(user.postsCount),
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

  override def getRefreshTokenByUserId(userId: String): Future[Option[(Option[RefreshToken], LoginInfo)]] = {
    userDAO.findById(userId).map { userOpt =>
      userOpt.map(user => (user.refreshToken, user.loginInfo))
    }
  }

}
