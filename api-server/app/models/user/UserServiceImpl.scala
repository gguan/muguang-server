package models.user

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.muguang.util.RandomUtils
import models.post.{ PostService, PostDAO }
import models.{ RefreshToken, UserSummary, User }
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import module.sihouette.WeiboProfile

import scala.concurrent.Future

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserServiceImpl @Inject() (userDAO: UserDAO, postService: PostService) extends UserService {

  /**
   * Retrieves a user that matches the specified login info.
   *
   * @param loginInfo The login info to retrieve a user.
   * @return The retrieved user or None if no user could be retrieved for the given login info.
   */
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userDAO.find(loginInfo)

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) = userDAO.save(user)

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  def save(profile: CommonSocialProfile) = {
    userDAO.find(profile.loginInfo).flatMap {
      case Some(user) =>
        // Not update if find user with profile, only update refresh_token
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

  /**
   * Saves the weibo profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  override def save(profile: WeiboProfile): Future[User] = {
    userDAO.find(profile.loginInfo).flatMap {
      case Some(user) =>
        // Not update if find user with profile, only update refresh_token
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

  override def follow(followed: String, follower: String): Future[Boolean] = {
    val query = Json.obj("$addToSet" -> Json.obj("f" -> followed))
    userDAO.update(follower, query).map {
      result =>
        result match {
          case Right(b) => true
          case Left(ex) => false
        }
    }
  }

  override def unfollow(unfollowed: String, follower: String): Future[Boolean] = {
    userDAO.pull(follower, "following", unfollowed).map {
      result =>
        result match {
          case Right(b) => true
          case Left(ex) => false
        }
    }
  }

  override def loadUserSummary(userId: String): Future[Option[UserSummary]] = {
    userDAO.findById(userId).flatMap(uOpt => uOpt match {
      case Some(user) =>
        for {
          countPosts <- postService.countPostByUserId(user.identify)
          countFollowers <- userDAO.countFollowers(user.identify)
        } yield {
          Some(UserSummary(user.identify,
            user.screenName,
            user.avatarUrl,
            user.biography,
            user.location,
            Some(countPosts),
            Some(user.following.size),
            Some(countFollowers)))
        }
      case None => Future.successful(None)
    })
  }

  def getUserRefreshTokenWithLoginInfo(userId: String): Future[Option[(Option[RefreshToken], LoginInfo)]] = {
    userDAO.findById(userId).map { userOpt =>
      userOpt.map(user => (user.refreshToken, user.loginInfo))
    }
  }

  override def getFollowers(userId: String, skip: Int, limit: Int): Future[List[UserSummary]] = {
    userDAO.getFollowers(userId, skip, limit).map { list =>
      list.map(u => UserSummary(u._id.stringify, u.screenName, u.avatarUrl, u.biography, u.location))
    }
  }

  override def getFollowings(userId: String, skip: Int, limit: Int): Future[List[UserSummary]] = {
    userDAO.findById(userId).flatMap(userOpt => userOpt match {
      case Some(user) => {
        userDAO.findUsersByIds(user.following, skip, limit).map { list =>
          list.map(u => UserSummary(u._id.stringify, u.screenName, u.avatarUrl, u.biography, u.location))
        }
      }
      case None => Future.successful(List())
    })
  }

}
