package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ LoginInfo, Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import models.User
import models.user.UserService
import play.api.libs.json.Json
import play.api.mvc.Action
import reactivemongo.bson.BSONObjectID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The basic application controller.
 *
 * @param env The Silhouette environment.
 */
class ApplicationController @Inject() (
  implicit val env: Environment[User, SessionAuthenticator],
  val userService: UserService) extends Silhouette[User, SessionAuthenticator] {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = SecuredAction.async { implicit request =>
    Future.successful(Ok("ok"))
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut = SecuredAction.async { implicit request =>
    val result = Future.successful(Ok("sign out successfully"))
    env.eventBus.publish(LogoutEvent(request.identity, request, request2lang))

    request.authenticator.discard(result)
  }

  def test = Action.async { implicit request =>
    val u = User(
      _id = Some(BSONObjectID.generate),
      loginInfo = LoginInfo("a", "b"),
      username = "test user",
      email = Some("g@example.com")
    )
    val result = for {
      user <- userService.save(u)
      summary <- userService.loadUserSummary(u.identify)
    } yield {
      summary
    }
    result.map(s => Ok(Json.toJson(s)))
  }
}
