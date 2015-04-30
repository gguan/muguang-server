package module.di

import com.google.inject.{ AbstractModule, Provides, Singleton }
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{ Environment, EventBus }
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.daos.CacheAuthenticatorDAO
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth2._
import com.mohiva.play.silhouette.impl.providers.oauth2.state.DummyStateProvider
import com.mohiva.play.silhouette.impl.services._
import com.mohiva.play.silhouette.impl.util._
import models.User
import models.timeline._
import models.user._
import models.post._
import net.codingwell.scalaguice.ScalaModule
import play.api.Play
import play.api.Play.current
import module.sihouette.{ RedisCacheLayer, WeiboProvider }
import services.{ TimelineService, PostService, UserGraphService }
import scala.collection.immutable.ListMap

/**
 * The Guice module which wires all Silhouette dependencies.
 */
class SilhouetteModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  def configure() {
    bind[UserGraphService].to[UserGraphServiceImpl]
    bind[UserDAO].toInstance(new UserDAOImpl())
    bind[PostService].to[PostServiceImpl]
    bind[PostDAO].toInstance(new PostDAOImpl())
    bind[TimelineService].to[TimelineServiceImpl]
    bind[TimelineDAO].toInstance(new TimelineDAOImpl())
    bind[CacheLayer].to[RedisCacheLayer]
    bind[HTTPLayer].to[PlayHTTPLayer]
    bind[OAuth2StateProvider].to[DummyStateProvider]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator(32))
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
  @Provides @Singleton
  def provideEnvironment(
    userService: UserGraphService,
    authenticatorService: AuthenticatorService[JWTAuthenticator],
    eventBus: EventBus,
    facebookProvider: FacebookProvider,
    weiboProvider: WeiboProvider): Environment[User, JWTAuthenticator] = {

    Environment[User, JWTAuthenticator](
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
   * @param idGenerator The ID generator used to create the authenticator ID.
   * @return The authenticator service.
   */
  @Provides
  def provideAuthenticatorService(
    cacheLayer: CacheLayer,
    idGenerator: IDGenerator,
    fingerprintGenerator: FingerprintGenerator): AuthenticatorService[JWTAuthenticator] = {

    new JWTAuthenticatorService(JWTAuthenticatorSettings(
      headerName = Play.configuration.getString("silhouette.authenticator.headerName").get,
      issuerClaim = Play.configuration.getString("silhouette.authenticator.issuerClaim").get,
      encryptSubject = Play.configuration.getBoolean("silhouette.authenticator.encryptSubject").get,
      authenticatorExpiry = Play.configuration.getInt("silhouette.authenticator.authenticatorExpiry").get,
      sharedSecret = Play.configuration.getString("application.secret").get
    ), Some(new CacheAuthenticatorDAO[JWTAuthenticator](cacheLayer)), idGenerator, Clock())
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
