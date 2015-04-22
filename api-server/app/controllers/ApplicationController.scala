package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User
import models.user.UserService
import play.api.mvc.Action
import scala.concurrent.Future

/**
 * The basic application controller.
 *
 * @param env The Silhouette environment.
 */
class ApplicationController @Inject() (
  implicit val env: Environment[User, JWTAuthenticator],
  val userService: UserService) extends Silhouette[User, JWTAuthenticator] {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = Action.async { implicit request =>
    Future.successful(Ok("running"))
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

  def test1 = Action.async {
    Future.successful(Ok)
  }

  def test2 = SecuredAction.async { implicit request =>
    //    val u = User(
    //      _id = Some(BSONObjectID.generate),
    //      loginInfo = LoginInfo("a", "b"),
    //      username = "test user",
    //      email = Some("g@example.com")
    //    )
    //    val result = for {
    //      user <- userService.save(u)
    //      summary <- userService.loadUserSummary(u.identify)
    //    } yield {
    //      summary
    //    }
    //    result.map(s => Ok(Json.toJson(s)))
    //    val result = for {
    //      user <- userService.retrieve(LoginInfo("a", "b"))
    //      summary <- userService.loadUserSummary(user.map(_.identify).getOrElse("unknown"))
    //    } yield {
    //      summary
    //    }
    //    result.map(s => Ok(Json.toJson(s)))
    Future.successful(Ok("test successfully"))
  }
}
