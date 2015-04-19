package models.user

import com.mohiva.play.silhouette.api.LoginInfo
import com.muguang.core.dao.BaseDocumentDao
import com.muguang.core.db.DBQueryBuilder
import models.User
import play.api.libs.json.Json
import reactivemongo.api.indexes.IndexType

import scala.concurrent.Future

/**
 * Give access to the user object.
 */
class UserDAOImpl extends UserDAO with BaseDocumentDao[User] {

  override val collectionName: String = "users"

  override def ensureIndexes: Future[List[Boolean]] = {
    for {
      usernameIndex <- ensureIndex(List(("li.pid", IndexType.Ascending), ("li.pk", IndexType.Ascending)))
      followingIndex <- ensureIndex(List(("f", IndexType.Ascending)))
    } yield {
      List(usernameIndex)
    }
  }

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo) = {
    findOne(
      Json.obj("li" ->
        Json.obj("pid" -> loginInfo.providerID, "pk" -> loginInfo.providerKey)
      )
    )
  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) = {
    insert(user).map {
      result =>
        result match {
          case Right(document) => document
          case Left(ex) => throw ex
        }
    }
  }

  override def countFollowers(userId: String): Future[Int] = {
    count(DBQueryBuilder.in("f", Seq(userId)))
  }

  def getFollowers(userId: String, skip: Int, limit: Int): Future[List[User]] = {
    findWithOptions(DBQueryBuilder.in("f", Seq(userId)), skip, limit)
  }

  override def findUsersByIds(userIds: Seq[String], skip: Int, limit: Int): Future[List[User]] = {
    findWithOptions(DBQueryBuilder.byIds(userIds), skip, limit)
  }
}

