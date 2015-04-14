package utils.di

import com.google.inject.{ AbstractModule, Provides }
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{ Environment, EventBus }
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth1._
import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.{ CookieSecretSettings, CookieSecretProvider }
import com.mohiva.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
import com.mohiva.play.silhouette.impl.providers.oauth2._
import com.mohiva.play.silhouette.impl.providers.oauth2.state.{ DummyStateProvider, CookieStateProvider, CookieStateSettings }
import com.mohiva.play.silhouette.impl.services._
import com.mohiva.play.silhouette.impl.util._
import models.User
import models.daos._
import models.user.{ UserServiceImpl, UserService, UserDAOImpl, UserDAO }
import net.codingwell.scalaguice.ScalaModule
import play.api.Play
import play.api.Play.current
import utils.oauth2.WeiboProvider

import scala.collection.immutable.ListMap

/**
 * The Guice module which wires all Silhouette dependencies.
 */
class SilhouetteModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  def configure() {
    bind[UserService].to[UserServiceImpl]
    bind[UserDAO].to[UserDAOImpl]
    bind[DelegableAuthInfoDAO[OAuth1Info]].to[OAuth1InfoDAO]
    bind[DelegableAuthInfoDAO[OAuth2Info]].to[OAuth2InfoDAO]
    bind[DelegableAuthInfoDAO[OpenIDInfo]].to[OpenIDInfoDAO]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[HTTPLayer].to[PlayHTTPLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[PasswordHasher].toInstance(new BCryptPasswordHasher)
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
  }

  /**
   * Provides the Silhouette environment.
   *
   * @param userService The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus The event bus instance.
   * @param facebookProvider The Facebook provider implementation.
   * @param weiboProvider The Weibo provider implementation.
   * @return The Silhouette environment.
   */
  @Provides
  def provideEnvironment(
    userService: UserService,
    authenticatorService: AuthenticatorService[SessionAuthenticator],
    eventBus: EventBus,
    facebookProvider: FacebookProvider,
    weiboProvider: WeiboProvider): Environment[User, SessionAuthenticator] = {

    Environment[User, SessionAuthenticator](
      userService,
      authenticatorService,
      ListMap(
        facebookProvider.id -> facebookProvider,
        weiboProvider.id -> weiboProvider
      ),
      eventBus
    )
  }

  /**
   * Provides the authenticator service.
   *
   * @param fingerprintGenerator The fingerprint generator implementation.
   * @return The authenticator service.
   */
  @Provides
  def provideAuthenticatorService(
    fingerprintGenerator: FingerprintGenerator): AuthenticatorService[SessionAuthenticator] = {
    new SessionAuthenticatorService(SessionAuthenticatorSettings(
      sessionKey = Play.configuration.getString("silhouette.authenticator.sessionKey").get,
      encryptAuthenticator = Play.configuration.getBoolean("silhouette.authenticator.encryptAuthenticator").get,
      useFingerprinting = Play.configuration.getBoolean("silhouette.authenticator.useFingerprinting").get,
      authenticatorIdleTimeout = Play.configuration.getInt("silhouette.authenticator.authenticatorIdleTimeout"),
      authenticatorExpiry = Play.configuration.getInt("silhouette.authenticator.authenticatorExpiry").get
    ), fingerprintGenerator, Clock())
  }

  /**
   * Provides the auth info service.
   *
   * @param oauth1InfoDAO The implementation of the delegable OAuth1 auth info DAO.
   * @param oauth2InfoDAO The implementation of the delegable OAuth2 auth info DAO.
   * @param openIDInfoDAO The implementation of the delegable OpenID auth info DAO.
   * @return The auth info service instance.
   */
  @Provides
  def provideAuthInfoService(
    oauth1InfoDAO: DelegableAuthInfoDAO[OAuth1Info],
    oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info],
    openIDInfoDAO: DelegableAuthInfoDAO[OpenIDInfo]): AuthInfoService = {

    new DelegableAuthInfoService(oauth1InfoDAO, oauth2InfoDAO, openIDInfoDAO)
  }

  /**
   * Provides the avatar service.
   *
   * @param httpLayer The HTTP layer implementation.
   * @return The avatar service implementation.
   */
  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService = new GravatarService(httpLayer)

  /**
   * Provides the OAuth1 token secret provider.
   *
   * @return The OAuth1 token secret provider implementation.
   */
  @Provides
  def provideOAuth1TokenSecretProvider: OAuth1TokenSecretProvider = {
    new CookieSecretProvider(CookieSecretSettings(
      cookieName = Play.configuration.getString("silhouette.oauth1TokenSecretProvider.cookieName").get,
      cookiePath = Play.configuration.getString("silhouette.oauth1TokenSecretProvider.cookiePath").get,
      cookieDomain = Play.configuration.getString("silhouette.oauth1TokenSecretProvider.cookieDomain"),
      secureCookie = Play.configuration.getBoolean("silhouette.oauth1TokenSecretProvider.secureCookie").get,
      httpOnlyCookie = Play.configuration.getBoolean("silhouette.oauth1TokenSecretProvider.httpOnlyCookie").get,
      expirationTime = Play.configuration.getInt("silhouette.oauth1TokenSecretProvider.expirationTime").get
    ), Clock())
  }

  /**
   * Provides the OAuth2 state provider.
   *
   * @param idGenerator The ID generator implementation.
   * @return The OAuth2 state provider implementation.
   */
  @Provides
  def provideOAuth2StateProvider(idGenerator: IDGenerator): OAuth2StateProvider = {
    new CookieStateProvider(CookieStateSettings(
      cookieName = Play.configuration.getString("silhouette.oauth2StateProvider.cookieName").get,
      cookiePath = Play.configuration.getString("silhouette.oauth2StateProvider.cookiePath").get,
      cookieDomain = Play.configuration.getString("silhouette.oauth2StateProvider.cookieDomain"),
      secureCookie = Play.configuration.getBoolean("silhouette.oauth2StateProvider.secureCookie").get,
      httpOnlyCookie = Play.configuration.getBoolean("silhouette.oauth2StateProvider.httpOnlyCookie").get,
      expirationTime = Play.configuration.getInt("silhouette.oauth2StateProvider.expirationTime").get
    ), idGenerator, Clock())
  }

  /**
   * Provides the Facebook provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param stateProvider The OAuth2 state provider implementation.
   * @return The Facebook provider.
   */
  @Provides
  def provideFacebookProvider(httpLayer: HTTPLayer, stateProvider: OAuth2StateProvider): FacebookProvider = {
    FacebookProvider(httpLayer, stateProvider, OAuth2Settings(
      authorizationURL = Play.configuration.getString("silhouette.facebook.authorizationURL"),
      accessTokenURL = Play.configuration.getString("silhouette.facebook.accessTokenURL").get,
      redirectURL = Play.configuration.getString("silhouette.facebook.redirectURL").get,
      clientID = Play.configuration.getString("silhouette.facebook.clientID").getOrElse(""),
      clientSecret = Play.configuration.getString("silhouette.facebook.clientSecret").getOrElse(""),
      scope = Play.configuration.getString("silhouette.facebook.scope")))
  }

  /**
   * Provides the Facebook provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param stateProvider The OAuth2 state provider implementation.
   * @return The Facebook provider.
   */
  @Provides
  def provideWeiboProvider(httpLayer: HTTPLayer, stateProvider: OAuth2StateProvider): WeiboProvider = {
    WeiboProvider(httpLayer, stateProvider, OAuth2Settings(
      authorizationURL = Play.configuration.getString("silhouette.facebook.authorizationURL"),
      accessTokenURL = Play.configuration.getString("silhouette.facebook.accessTokenURL").get,
      redirectURL = Play.configuration.getString("silhouette.facebook.redirectURL").get,
      clientID = Play.configuration.getString("silhouette.facebook.clientID").getOrElse(""),
      clientSecret = Play.configuration.getString("silhouette.facebook.clientSecret").getOrElse(""),
      scope = Play.configuration.getString("silhouette.facebook.scope")))
  }

}
