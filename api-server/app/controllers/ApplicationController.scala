package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User
import play.api.mvc.Action
import services.UserGraphService
import scala.concurrent.Future

/**
 * The basic application controller.
 *
 * @param env The Silhouette environment.
 */
class ApplicationController @Inject() (
  implicit val env: Environment[User, JWTAuthenticator],
  val userService: UserGraphService) extends Silhouette[User, JWTAuthenticator] {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = Action.async { implicit request =>
    Future.successful(Ok("running"))
  }

  def test1 = Action.async {
    Future.successful(Ok)
  }

  def test2 = SecuredAction.async { implicit request =>
    Future.successful(Ok("secured"))
  }
}
