package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import com.github.nscala_time.time.Imports._
import models.User
import models.user.UserService
import module.sihouette.{ WeiboProfileBuilder, WeiboProvider }
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.Action

import scala.concurrent.Future

/**
 * The social auth controller.
 *
 * @param env The Silhouette environment.
 */
class SocialAuthController @Inject() (
  val env: Environment[User, JWTAuthenticator],
  val userService: UserService) extends Silhouette[User, JWTAuthenticator] with Logger {

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticateByWeb(provider: String) = Action.async { implicit request =>
    (env.providers.get(provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => for {
            // Use authInfo to retrieve user profile
            profile <- p.retrieveProfile(authInfo)
            // Create new account if user not exists
            user <- userService.save(profile)
            // Create client access token
            authenticator <- env.authenticatorService.create(user.loginInfo)
            // Cache access token
            value <- env.authenticatorService.init(authenticator)
            result <- env.authenticatorService.embed(value, Future.successful(
              Ok("authenticate successfully.")
            ))
          } yield {
            env.eventBus.publish(AuthenticatedEvent(user, request, request2lang))
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

  def authenticate(provider: String) = Action.async(parse.json) { implicit request =>

    val idOpt = (request.body \ "uid").asOpt[String]
    val authInfoOpt = (request.body \ "oauth2_info").asOpt[OAuth2Info]

    (idOpt, authInfoOpt) match {
      case (Some(id), Some(authInfo)) => {
        val params = authInfo.params.getOrElse(Map[String, String]()) + ("uid" -> id)
        val authInfoWithId = authInfo.copy(params = Some(params))

        (env.providers.get(provider) match {
          case Some(p: WeiboProvider with WeiboProfileBuilder) =>
            for {
              // Use authInfo to retrieve user profile
              profile <- p.retrieveProfile(authInfoWithId)
              // Create new account if user not exists
              user <- userService.save(profile)
              // Create client access token
              authenticator <- env.authenticatorService.create(user.loginInfo)
              // Cache access token
              value <- env.authenticatorService.init(authenticator)
              result <- env.authenticatorService.embed(value, Future.successful(
                Ok(Json.obj(
                  "uid" -> id,
                  "access_token" -> value,
                  "expires_in" -> (DateTime.now to authenticator.expirationDate).millis / 1000,
                  "refresh_token" -> user.refreshToken.map(_.token).getOrElse(null)
                ))
              ))
            } yield {
              env.eventBus.publish(AuthenticatedEvent(user, request, request2lang))
              result
            }
          case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
        }).recover {
          case e: ProviderException =>
            logger.error("Unexpected provider error", e)
            BadRequest(Json.obj("error" -> Messages("could.not.authenticate")))
        }
      }
      case _ => {
        Future.successful(BadRequest(Json.obj(
          "error" -> "missing id or access_token"
        )))
      }
    }
  }

  def refreshToken() = Action.async(parse.json) { implicit request =>
    val idOpt = (request.body \ "uid").asOpt[String]
    val rtOpt = (request.body \ "refresh_token").asOpt[String]

    (idOpt, rtOpt) match {
      case (Some(id), Some(refreshToken)) => {
        userService.getUserRefreshTokenWithLoginInfo(id).flatMap(t => t match {
          case Some((Some(token), loginInfo)) => {
            if (token.isValid && token.token == refreshToken) {
              for {
                authenticator <- env.authenticatorService.create(loginInfo)
                value <- env.authenticatorService.init(authenticator)
                result <- env.authenticatorService.embed(value, Future.successful(
                  Ok(Json.obj(
                    "uid" -> id,
                    "access_token" -> value,
                    "expires_in" -> (DateTime.now to authenticator.expirationDate).millis / 1000
                  ))
                ))
              } yield {
                result
              }
            } else {
              Future.successful(Unauthorized(Json.obj("error" -> Messages("error.401"))))
            }
          }
          case _ => Future.successful(Unauthorized(Json.obj("error" -> Messages("error.401"))))
        })
      }
      case _ => Future.successful(Unauthorized(Json.obj("error" -> Messages("error.401"))))
    }
  }

}
