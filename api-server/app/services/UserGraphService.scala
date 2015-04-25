package services

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.{ RefreshToken, User, UserSummary }
import module.sihouette.WeiboProfile
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

/**
 * Handles actions to users.
 */
trait UserGraphService extends IdentityService[User] {

  /**
   * Find a user by userId
   * @param userId the id of the target user
   * @return the user object for the user with provided id if one exists
   */
  def retrieve(userId: BSONObjectID): Future[Option[User]]

  /**
   * Saves a user.
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User): Future[User]

  /**
   * Saves the social profile for a user.
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  def save(profile: CommonSocialProfile): Future[User]

  /**
   * Saves the weibo profile for a user.
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   * @param profile The weibo profile to save.
   * @return The user for whom the profile was saved.
   */
  def save(profile: WeiboProfile): Future[User]

  /**
   * Check a user ID refers to a valid user
   * @param userId the target userId. If the user
   * is invalid, a service exception is thrown.
   * @return
   */
  def validateUser(userId: String): Future[User]

  /**
   * Establish a follow relationship between two users
   * @param from the user id that is following
   * @param to the user id to be followed
   * @return
   */
  def follow(from: BSONObjectID, to: BSONObjectID): Future[Unit]

  /**
   * Remove a follows relationship from the graph
   * @param from the user id that is following
   * @param to the user id being followed
   * @return
   */
  def unfollow(from: BSONObjectID, to: BSONObjectID): Future[Unit]

  /**
   * Determine how many followers a user has
   * @param userId the target user id
   * @return the number of followers for the user or zero
   * if the user does not exist
   */
  def getFollowersCount(userId: BSONObjectID): Future[Int]

  /**
   * Retrieve a list of followers for the user
   * @param userId the target user id
   * @param skip number to skip
   * @param limit number limited to return
   * @return a list of UserSummary objects representing the followers
   * of the target user or an empty list if the user does not exist
   */
  def getFollowers(userId: BSONObjectID, skip: Int = 0, limit: Int = Int.MaxValue): Future[List[BSONObjectID]]

  /**
   * Determine how many users a target is following
   * @param userId the target user id
   * @return the number of users the target user is following
   * or zero if the target user does not exist
   */
  def getFollowingCount(userId: BSONObjectID): Future[Int]

  /**
   * Retrieve a list of users a target user is following
   * @param userId the target user id
   * @param skip number to skip
   * @param limit number limited to return
   * @return a list of UserSummary objects representing the users
   * that the target user is following or an empty list if
   * the user does not exist
   */
  def getFollowing(userId: BSONObjectID, skip: Int, limit: Int): Future[List[BSONObjectID]]

  /**
   * Check if a user is following another user
   * @param from user id that is being followed
   * @param to user id that is following
   * @return if user is following target user(1) isfollowed(-1) or not following(0) or both following each other(2)
   */
  def isFollowing(from: BSONObjectID, to: BSONObjectID): Future[Int]

  /**
   * Load user summary by given user id
   * @param userId target user id
   * @return UserSummary object contains summary information
   */
  def getUserSummary(userId: BSONObjectID): Future[UserSummary]

  /**
   * Load user summary by given user ids
   * @param ids ids of users
   * @return UserSummary object list
   */
  def getUerSummaryByIds(ids: List[BSONObjectID]): Future[List[UserSummary]]

  /**
   * Retrieve refresh token by give user id
   * @param userId target user id
   * @return refresh token
   */
  def getRefreshTokenByUserId(userId: String): Future[Option[(Option[RefreshToken], LoginInfo)]]

}
