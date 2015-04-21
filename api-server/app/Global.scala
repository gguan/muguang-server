package app

import com.google.inject.Guice
import com.mohiva.play.silhouette.api.{ Logger, SecuredSettings }
import models.post.PostDAOImpl
import models.user.UserDAOImpl
import play.api._
import play.api.i18n.{ Lang, Messages }
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{ WithFilters, RequestHeader, Result }
import play.filters.gzip.GzipFilter
import module.di.SilhouetteModule

import scala.concurrent.Future

/**
 * The global object.
 */
object Global extends WithFilters(new GzipFilter()) with Global

/**
 * The global configuration.
 */
trait Global extends GlobalSettings with SecuredSettings with Logger {

  /**
   * The Guice dependencies injector.
   */
  val injector = Guice.createInjector(new SilhouetteModule)

  /**
   * Loads the controller classes with the Guice injector,
   * in order to be able to inject dependencies directly into the controller.
   *
   * @param controllerClass The controller class to instantiate.
   * @return The instance of the controller class.
   */
  override def getControllerInstance[A](controllerClass: Class[A]) = injector.getInstance(controllerClass)

  /**
   * Called when a user is not authenticated.
   *
   * As defined by RFC 2616, the status code of the response should be 401 Unauthorized.
   *
   * @param request The request header.
   * @param lang The currently selected language.
   * @return The result to send to the client.
   */
  override def onNotAuthenticated(request: RequestHeader, lang: Lang): Option[Future[Result]] = {
    Some(Future.successful(Forbidden(Json.obj("error" -> Messages("error.401")))))
  }

  /**
   * Called when a user is authenticated but not authorized.
   *
   * As defined by RFC 2616, the status code of the response should be 403 Forbidden.
   *
   * @param request The request header.
   * @param lang The currently selected language.
   * @return The result to send to the client.
   */
  override def onNotAuthorized(request: RequestHeader, lang: Lang): Option[Future[Result]] = {
    Some(Future.successful(Unauthorized(Json.obj("error" -> Messages("error.403")))))
  }

  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    Future.successful(NotFound(Json.obj("error" -> "404 Not Found")))
  }

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    Future.successful(BadRequest(Json.obj("error" -> "400 Bad Request", "message" -> error)))
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(
      Json.obj("error" -> "400 Bad Request", "message" -> ex.getMessage)
    ))
  }

  override def onStart(app: Application) {
    injector.getInstance(classOf[UserDAOImpl]).ensureIndexes
    injector.getInstance(classOf[PostDAOImpl]).ensureIndexes
  }

}
