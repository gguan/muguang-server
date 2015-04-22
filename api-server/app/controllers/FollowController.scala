package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Logger, Silhouette, Environment }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User
import models.user.UserService

import scala.concurrent.ExecutionContext.Implicits.global

class FollowController @Inject() (
  val env: Environment[User, JWTAuthenticator],
  val userService: UserService) extends Silhouette[User, JWTAuthenticator] with Logger {

  def follow(id: String) = SecuredAction.async { implicit request =>
    userService.follow(id, request.identity.identify).map { result =>
      if (result) Ok
      else BadRequest
    }
  }

  def unfollow(id: String) = SecuredAction.async { implicit request =>
    userService.unfollow(id, request.identity.identify).map { result =>
      if (result) Ok
      else BadRequest
    }
  }

}
