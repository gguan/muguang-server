package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Logger, Silhouette, Environment }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User
import play.api.libs.json.Json
import services.UserGraphService
import scala.concurrent.ExecutionContext.Implicits.global

class BlockController @Inject() (
  implicit val env: Environment[User, JWTAuthenticator],
  val userService: UserGraphService)
  extends Silhouette[User, JWTAuthenticator] with Logger {

  def blacklist(id: String) = SecuredAction.async { implicit request =>
    for {
      user <- userService.validateUser(id)
      result <- userService.blacklist(request.identity._id, user._id)
    } yield {
      Ok
    }
  }

  def isInBlacklist(id: String) = SecuredAction.async { implicit request =>
    for {
      user <- userService.validateUser(id)
      result <- userService.isInBlacklist(request.identity._id, user._id)
    } yield {
      Ok(Json.obj("in_blacklist" -> result))
    }
  }

  def blockChat(id: String) = SecuredAction.async { implicit request =>
    for {
      user <- userService.validateUser(id)
      result <- userService.blockChat(request.identity._id, user._id)
    } yield {
      Ok
    }
  }

  def isBlockedChat(id: String) = SecuredAction.async { implicit request =>
    for {
      user <- userService.validateUser(id)
      result <- userService.isBlocked(request.identity._id, user._id)
    } yield {
      Ok(Json.obj("is_blocked" -> result))
    }
  }

}
