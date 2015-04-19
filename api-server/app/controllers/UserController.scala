package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Logger, Silhouette, Environment }
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import models.User
import models.user.UserService
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global

class UserController @Inject() (
  val env: Environment[User, BearerTokenAuthenticator],
  val userService: UserService) extends Silhouette[User, BearerTokenAuthenticator] with Logger {

  def getFollowers(id: String, skip: Int, limit: Int) = SecuredAction.async {
    userService.getFollowers(id, skip, limit).map(list => Ok(Json.toJson(list)))
  }

  def getFollowings(id: String, skip: Int, limit: Int) = SecuredAction.async {
    userService.getFollowings(id, skip, limit).map(list => Ok(Json.toJson(list)))
  }

  def getUserSummary(id: String) = SecuredAction.async { implicit request =>
    userService.loadUserSummary(id).map(userSummary => Ok(Json.toJson(userSummary)))
  }

}