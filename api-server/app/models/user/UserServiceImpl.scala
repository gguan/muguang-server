package models.user

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.muguang.util.RandomUtils
import models.{ UserSummary, User }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import utils.sihouette.WeiboProfile

import scala.concurrent.Future

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserServiceImpl @Inject() (userDAO: UserDAO) extends UserService {

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
        userDAO.update(user.identify, user.copy(refreshToken = Some(RandomUtils.generateToken())))
          .map(result => result match {
            case Left(ex) => throw ex
            case Right(u) => u
          })
      case None => // Insert a new user
        userDAO.save(User(
          _id = BSONObjectID.generate,
          loginInfo = profile.loginInfo,
          refreshToken = Some(RandomUtils.generateToken()),
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
        userDAO.update(user.identify, user.copy(refreshToken = Some(RandomUtils.generateToken())))
          .map(result => result match {
            case Left(ex) => throw ex
            case Right(u) => u
          })
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

  override def follow(followed: String, follower: String): Unit = {
    val query = Json.obj("$addToSet" -> Json.obj("f" -> followed))
    userDAO.update(follower, query)
  }

  override def unfollow(unfollowed: String, follower: String): Unit = {
    userDAO.pull(follower, "following", unfollowed)
  }

  override def loadUserSummary(userId: String): Future[UserSummary] = {
    for {
      user <- userDAO.findById(userId)
      //      countTweets <- tweetService.countTweets(user.get.identify)
      //      countFollowers <- userDAO.countFollowers(user.get.identify)
    } yield UserSummary(user.get.identify,
      user.get.screenName,
      user.get.avatarUrl,
      user.get.biography,
      0,
      user.get.following.size,
      0)
  }

  def getUserRefreshTokenWithLoginInfo(userId: String): Future[Option[(Option[String], LoginInfo)]] = {
    userDAO.findById(userId).map { userOpt =>
      userOpt.map(user => (user.refreshToken, user.loginInfo))
    }
  }

}
