package models.user

import com.mohiva.play.silhouette.api.LoginInfo
import com.muguang.core.dao.DocumentDao
import models.User
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

/**
 * Give access to the user object.
 */
trait UserDAO extends DocumentDao[User] {

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo): Future[Option[User]]

  /**
   * Finds a user by its user ID.
   *
   * @param userId The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userId: BSONObjectID): Future[Option[User]]

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User): Future[User]

  def countFollowers(userId: String): Future[Int]

  def findUsersByIds(userIds: List[String]): Future[List[User]]

}
