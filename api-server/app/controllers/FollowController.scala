package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Logger, Silhouette, Environment }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User
import services.UserGraphService

import scala.concurrent.ExecutionContext.Implicits.global

class FollowController @Inject() (
  val env: Environment[User, JWTAuthenticator],
  val userService: UserGraphService) extends Silhouette[User, JWTAuthenticator] with Logger {

  def follow(id: String) = SecuredAction.async { implicit request =>
    for {
      user <- userService.validateUser(id)
      result <- userService.follow(request.identity._id, user._id)
    } yield {
      Ok
    }
  }

  def unfollow(id: String) = SecuredAction.async { implicit request =>
    for {
      user <- userService.validateUser(id)
      result <- userService.unfollow(request.identity._id, user._id)
    } yield {
      Ok
    }
  }

}
