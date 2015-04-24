package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Logger, Silhouette, Environment }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User
import models.post.PostService
import models.user.UserService
import play.api.libs.json.Json
import play.api.mvc.Action
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserController @Inject() (
  val env: Environment[User, JWTAuthenticator],
  val userService: UserService,
  val postService: PostService) extends Silhouette[User, JWTAuthenticator] with Logger {

  def completeProfile() = SecuredAction.async(parse.json) { implicit request =>
    Future.successful(Ok)
  }

  def getFollowers(id: String, skip: Int, limit: Int) = SecuredAction.async {
    userService.getFollowers(id, skip, limit).map(list => Ok(Json.toJson(list)))
  }

  def getFollowings(id: String, skip: Int, limit: Int) = SecuredAction.async {
    userService.getFollowing(id, skip, limit).map(list => Ok(Json.toJson(list)))
  }

  def getUserSummary(id: String) = SecuredAction.async { implicit request =>
    userService.getUserSummary(id).map(userSummary => Ok(Json.toJson(userSummary)))
  }

  def getRecentPostsByUserId(userId: String, skip: Int, limit: Int) = Action.async { implicit request =>
    postService.getRecentPostsByUserId(userId, skip, limit).map(list => Ok(Json.toJson(list)))
  }

}