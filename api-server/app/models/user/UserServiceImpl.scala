package models.user

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.muguang.core.db.MongoHelper
import com.muguang.core.exceptions.ResourceNotFoundException
import com.muguang.util.RandomUtils
import models.post.PostService
import models.{ RefreshToken, UserSummary, User }
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import module.sihouette.WeiboProfile
import reactivemongo.core.commands.Count

import scala.concurrent.Future

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserServiceImpl @Inject() (userDAO: UserDAO, postService: PostService) extends UserService with MongoHelper {

  val followingCollection = db.collection[BSONCollection]("following")
  val followersCollection = db.collection[JSONCollection]("followers")

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userDAO.find(loginInfo)

  override def retrieve(userId: String): Future[Option[User]] = userDAO.findById(userId)

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
          screenName = profile.firstName + "_" + profile.lastName,
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

  override def validateUser(userId: String): Future[User] = {
    userDAO.findById(userId).map(userOpt => userOpt match {
      case Some(user) => user
      case None => throw ResourceNotFoundException(userId)
    })
  }

  override def follow(from: String, to: String): Future[Unit] = {

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

  override def unfollow(from: String, to: String): Future[Unit] = {

    for {
      following <- Recover(followingCollection.remove(BSONDocument("_f" -> from, "_t" -> to))) {}
      followers <- Recover(followersCollection.remove(BSONDocument("_f" -> to, "_t" -> from))) {}
      // update the following and follower counts of the two users respectively
      followingCount <- userDAO.update(from, Json.obj("$inc" -> Json.obj("_cfg" -> -1)))
      followersCount <- userDAO.update(to, Json.obj("$inc" -> Json.obj("_cfr" -> -1)))
    } yield {}
  }

  override def getFollowersCount(userId: String): Future[Int] = {
    followersCollection.db.command(
      Count(
        "followers",
        Some(BSONDocument("_f" -> userId))
      )
    )
  }

  override def getFollowers(userId: String, skip: Int, limit: Int): Future[List[UserSummary]] = {
    val futureIds = followersCollection
      .find(BSONDocument("_f" -> userId), BSONDocument("_t" -> 1))
      .cursor[BSONDocument]
      .collect[List]()

    for {
      ids <- futureIds.map(list => list.flatMap(_.getAs[String]("_t")))
      followers <- userDAO.findUsersByIds(ids, skip, limit)
    } yield {
      followers.map(u => UserSummary(Some(u._id.stringify), u.screenName, u.avatarUrl))
    }
  }

  override def getFollowingCount(userId: String): Future[Int] = {
    followersCollection.db.command(
      Count(
        "following",
        Some(BSONDocument("_f" -> userId))
      )
    )
  }

  override def getFollowings(userId: String, skip: Int, limit: Int): Future[List[UserSummary]] = {
    val futureIds = followingCollection
      .find(BSONDocument("_f" -> userId), BSONDocument("_t" -> 1))
      .cursor[BSONDocument]
      .collect[List]()

    for {
      ids <- futureIds.map(list => list.flatMap(_.getAs[String]("_t")))
      followers <- userDAO.findUsersByIds(ids, skip, limit)
    } yield {
      followers.map(u => UserSummary(Some(u._id.stringify), u.screenName, u.avatarUrl))
    }
  }

  override def isFollowing(from: String, to: String): Future[Int] = {
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

  override def getUserSummary(userId: String): Future[UserSummary] = {
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

  def getRefreshTokenByUserId(userId: String): Future[Option[(Option[RefreshToken], LoginInfo)]] = {
    userDAO.findById(userId).map { userOpt =>
      userOpt.map(user => (user.refreshToken, user.loginInfo))
    }
  }

}
