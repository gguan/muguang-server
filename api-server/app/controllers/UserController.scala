package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Logger, Silhouette, Environment }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.{ UserSummary, User }
import play.api.libs.json.Json
import services.{ PostService, UserGraphService }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserController @Inject() (
  implicit val env: Environment[User, JWTAuthenticator],
  val userService: UserGraphService,
  val postService: PostService) extends Silhouette[User, JWTAuthenticator] with Logger {

  def completeProfile() = SecuredAction.async(parse.json) { implicit request =>
    Future.successful(Ok)
  }

  def getFollowers(id: String, skip: Int, limit: Int) = SecuredAction.async {
    for {
      user <- userService.validateUser(id)
      ids <- userService.getFollowers(user._id, skip, limit)
      followers <- userService.getUerSummaryByIds(ids)
    } yield {
      Ok(Json.toJson(followers))
    }
  }

  def getFollowing(id: String, skip: Int, limit: Int) = SecuredAction.async {
    for {
      user <- userService.validateUser(id)
      ids <- userService.getFollowing(user._id, skip, limit)
      following <- userService.getUerSummaryByIds(ids)
    } yield {
      Ok(Json.toJson(following))
    }
  }

  def getUserSummary(id: String) = SecuredAction.async { implicit request =>
    for {
      user <- userService.validateUser(id)
    } yield {
      Ok(Json.toJson(
        UserSummary(
          Some(id),
          user.screenName,
          user.avatarUrl,
          user.biography,
          user.location,
          Some(user.postCount),
          Some(user.followingCount),
          Some(user.followersCount)
        )
      ))
    }
  }

}