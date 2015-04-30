package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Logger, Silhouette, Environment }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User
import reactivemongo.bson.BSONObjectID
import services.{ TimelineService, PostService }
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class TimelineController @Inject() (
  implicit val env: Environment[User, JWTAuthenticator],
  val postService: PostService,
  val timelineService: TimelineService) extends Silhouette[User, JWTAuthenticator] with Logger {

  def getTimeline(id: String, limit: Int, anchor: Option[String]) = SecuredAction.async { implicit request =>
    timelineService.getFeedFor(BSONObjectID(id), limit, anchor.map(BSONObjectID(_))).map(list => Ok(Json.toJson(list)))
  }

}
