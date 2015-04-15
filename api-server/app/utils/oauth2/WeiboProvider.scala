package utils.oauth2

import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.exceptions.{ UnexpectedResponseException, ProfileRetrievalException }
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.OAuth2Provider._

import utils.oauth2.WeiboProvider._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{ JsObject, JsValue }
import play.api.libs.ws.WSResponse

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

/**
 * A Weibo OAuth2 Provider.
 *
 * @param httpLayer The HTTP layer implementation.
 * @param stateProvider The state provider implementation.
 * @param settings The provider settings.
 *
 * @see http://open.weibo.com/wiki/2/users/show
 */
abstract class WeiboProvider(httpLayer: HTTPLayer, stateProvider: OAuth2StateProvider, settings: OAuth2Settings)
  extends OAuth2Provider(httpLayer, stateProvider, settings) {

  /**
   * The content type to parse a profile from.
   */
  type Content = JsValue

  /**
   * Gets the provider ID.
   *
   * @return The provider ID.
   */
  val id = ID

  /**
   * Defines the URLs that are needed to retrieve the profile data.
   */
  protected val urls = Map(
    "userInfoAPI" -> GetUerAPI,
    "userEmailApi" -> GetEmailAPI
  )

  /**
   * Builds the social profile.
   *
   * @param authInfo The auth info received from the provider.
   * @return On success the build social profile, otherwise a failure.
   */
  protected def buildProfile(authInfo: OAuth2Info): Future[Profile] = {
    val uid = authInfo.params.map(_.getOrElse("uid", "")).getOrElse("")
    httpLayer.url(urls("userInfoAPI").format(uid, authInfo.accessToken)).get().flatMap { response =>
      val json = response.json
      (json \ "error").asOpt[JsObject] match {
        case Some(error) =>
          val errorMsg = (error \ "message").as[String]
          val errorType = (error \ "type").as[String]
          val errorCode = (error \ "code").as[Int]

          throw new ProfileRetrievalException(SpecifiedProfileError.format(id, errorMsg, errorType, errorCode))
        case _ => profileParser.parse(json) // TODO: Need advanced privilege to get user's email
      }
    }
  }

  def getEmail(accessToken: String): Future[Option[String]] = {
    httpLayer.url(urls("userEmailAPI").format(accessToken)).get().map { response =>
      val json = response.json
      (json \ "error").asOpt[String] match {
        case Some(error) =>
          throw new ProfileRetrievalException(SpecifiedEmailError.format(id, error))
        case _ =>
          (json \ "email").asOpt[String].filter(!_.isEmpty)
      }
    } recover {
      case e: Exception =>
        None
    }
  }

  /**
   * Builds the OAuth2 info.
   *
   * Weibo does not follow the OAuth2 spec :-\
   *
   * @param response The response from the provider.
   * @return The OAuth2 info on success, otherwise an failure.
   */
  override protected def buildInfo(response: WSResponse): Try[OAuth2Info] = {
    response.body.split("&|=") match {
      case Array(AccessToken, token, Expires, expiresIn) => Success(OAuth2Info(token, None, Some(expiresIn.toInt)))
      case Array(AccessToken, token) => Success(OAuth2Info(token))
      case _ => Failure(new UnexpectedResponseException(InvalidInfoFormat.format(id, response.body)))
    }
  }
}

case class WeiboProfile(
  loginInfo: LoginInfo,
  userId: String,
  screenName: String,
  biography: Option[String] = None,
  location: Option[String] = None,
  gender: Option[String] = None,
  avatarUrl: Option[String] = None,
  email: Option[String] = None) extends SocialProfile

trait WeiboProfileBuilder extends SocialProfileBuilder {
  self: WeiboProvider =>

  /**
   * The type of the profile a profile builder is responsible for.
   */
  type Profile = WeiboProfile

  val profileParser = new WeiboProfileParser
}

/**
 * The profile parser for the weibo profile.
 */
class WeiboProfileParser extends SocialProfileParser[JsValue, WeiboProfile] {

  /**
   * Parses the social profile.
   *
   * @param json The content returned from the provider.
   * @return The social profile from given result.
   */
  def parse(json: JsValue) = Future.successful {

    val userId = (json \ "idstr").as[String]
    val screenName = (json \ "screen_name").as[String]
    val biography = (json \ "description").asOpt[String]
    val location = (json \ "location").asOpt[String]
    val gender = (json \ "gender").asOpt[String]
    val avatarUrl = (json \ "profile_image_url").asOpt[String]

    WeiboProfile(
      userId = userId,
      loginInfo = LoginInfo(ID, userId),
      screenName = screenName,
      biography = biography,
      location = location,
      gender = gender,
      avatarUrl = avatarUrl,
      email = None
    )
  }

}

/**
 * The companion object.
 */
object WeiboProvider {

  /**
   * The error messages.
   */
  val SpecifiedProfileError = "[Silhouette][%s] Error retrieving profile information. Error message: %s, type: %s, code: %s"

  /**
   * The error messages.
   */
  val SpecifiedEmailError = "[Silhouette][%s] Error retrieving email information. Error message: %s"

  /**
   * The Weibo constants.
   */
  val ID = "weibo"
  val GetUerAPI = "https://api.weibo.com/2/users/show.json?uid=%s&access_token=%s"
  val GetEmailAPI = "https://api.weibo.com/2/account/profile/email.json?access_token=%s"

  /**
   * Creates an instance of the provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param stateProvider The state provider implementation.
   * @param settings The provider settings.
   * @return An instance of this provider.
   */
  def apply(httpLayer: HTTPLayer, stateProvider: OAuth2StateProvider, settings: OAuth2Settings) = {
    new WeiboProvider(httpLayer, stateProvider, settings) with WeiboProfileBuilder
  }

}
