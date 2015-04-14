package utils.oauth2

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.exceptions.{ UnexpectedResponseException, ProfileRetrievalException }
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfileBuilder
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.impl.providers.OAuth2Provider
import com.mohiva.play.silhouette.impl.providers.OAuth2Provider._
import com.mohiva.play.silhouette.impl.providers.OAuth2Settings
import com.mohiva.play.silhouette.impl.providers.OAuth2StateProvider
import com.mohiva.play.silhouette.impl.providers.SocialProfileParser
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
 * @see https://developers.facebook.com/docs/facebook-login/access-tokens
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
  protected val urls = Map("api" -> GetUerAPI)

  /**
   * Builds the social profile.
   *
   * @param authInfo The auth info received from the provider.
   * @return On success the build social profile, otherwise a failure.
   */
  protected def buildProfile(authInfo: OAuth2Info): Future[Profile] = {
    httpLayer.url(urls("api").format(authInfo.accessToken)).get().flatMap { response =>
      val json = response.json
      (json \ "error").asOpt[JsObject] match {
        case Some(error) =>
          val errorMsg = (error \ "message").as[String]
          val errorType = (error \ "type").as[String]
          val errorCode = (error \ "code").as[Int]

          throw new ProfileRetrievalException(SpecifiedProfileError.format(id, errorMsg, errorType, errorCode))
        case _ => profileParser.parse(json)
      }
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

/**
 * The profile parser for the common social profile.
 */
class WeiboProfileParser extends SocialProfileParser[JsValue, CommonSocialProfile] {

  /**
   * Parses the social profile.
   *
   * @param json The content returned from the provider.
   * @return The social profile from given result.
   */
  def parse(json: JsValue) = Future.successful {
    val userID = (json \ "id").as[String]
    val firstName = (json \ "first_name").asOpt[String]
    val lastName = (json \ "last_name").asOpt[String]
    val fullName = (json \ "name").asOpt[String]
    val avatarURL = (json \ "picture" \ "data" \ "url").asOpt[String]
    val email = (json \ "email").asOpt[String]

    CommonSocialProfile(
      loginInfo = LoginInfo(ID, userID),
      firstName = firstName,
      lastName = lastName,
      fullName = fullName,
      avatarURL = avatarURL,
      email = email)
  }
}

/**
 * The profile builder for the common social profile.
 */
trait WeiboProfileBuilder extends CommonSocialProfileBuilder {
  self: WeiboProvider =>

  /**
   * The profile parser implementation.
   */
  val profileParser = new WeiboProfileParser
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
