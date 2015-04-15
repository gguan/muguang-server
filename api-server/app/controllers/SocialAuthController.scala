package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.services.AuthInfoService
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import com.muguang.util.RandomUtils
import models.User
import models.user.UserService
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.Action
import utils.oauth2.{ WeiboProvider, WeiboProfileBuilder }

import scala.concurrent.Future

/**
 * The social auth controller.
 *
 * @param env The Silhouette environment.
 */
class SocialAuthController @Inject() (
  val env: Environment[User, SessionAuthenticator],
  val userService: UserService,
  val authInfoService: AuthInfoService)
  extends Silhouette[User, SessionAuthenticator] with Logger {

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticate(provider: String) = Action.async { implicit request =>
    (env.providers.get(provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => for {
            profile <- p.retrieveProfile(authInfo)
            user <- userService.save(profile)
            authInfo <- authInfoService.save(profile.loginInfo, authInfo)
            authenticator <- env.authenticatorService.create(user.loginInfo)
            value <- env.authenticatorService.init(authenticator)
            result <- env.authenticatorService.embed(value, Future.successful(
              // TODO
              Ok("authenticate successfully.")
            ))
          } yield {
            env.eventBus.publish(LoginEvent(user, request, request2lang))
            result
          }
        }
      case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
    }).recover {
      case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        BadRequest(Json.obj("error" -> Messages("could.not.authenticate")))
    }
  }

  def verifySocialAuth(provider: String) = Action.async(parse.json) { implicit request =>

    val idOpt = (request.body \ "uid").asOpt[String]
    val authInfoOpt = (request.body \ "oauth2_info").asOpt[OAuth2Info]

    (idOpt, authInfoOpt) match {
      case (Some(id), Some(authInfo)) => {
        val params = authInfo.params.getOrElse(Map[String, String]()) + ("uid" -> id)
        val authInfoWithId = authInfo.copy(params = Some(params))

        (env.providers.get(provider) match {
          case Some(p: WeiboProvider with WeiboProfileBuilder) =>
            for {
              profile <- p.retrieveProfile(authInfoWithId)
              user <- userService.save(profile)
              authInfo <- authInfoService.save(profile.loginInfo, authInfo)
              authenticator <- env.authenticatorService.create(user.loginInfo)
              value <- env.authenticatorService.init(authenticator)
              result <- env.authenticatorService.embed(value, Future.successful(
                // TODO
                Ok("authenticate successfully.")
              ))
            } yield {
              env.eventBus.publish(LoginEvent(user, request, request2lang))
              result
            }
          case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
        }).recover {
          case e: ProviderException =>
            logger.error("Unexpected provider error", e)
            BadRequest(Json.obj("error" -> Messages("could.not.authenticate")))
        }

        // TODO:

        Future.successful(Ok(Json.obj(
          "id" -> id,
          "access_token" -> ""
        )))
      }
      case _ => {
        Future.successful(BadRequest(Json.obj(
          "error" -> "missing id or access_token"
        )))
      }
    }
  }

}
