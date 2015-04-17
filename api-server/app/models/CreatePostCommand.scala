package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.extras.geojson.{ LatLng, Feature }

case class CreatePostCommand(
  `type`: String,
  photos: Seq[PostPhoto],
  status: Option[String],
  location: Option[Feature[LatLng]],
  altitude: Option[Double])

object CreatePostCommand {
  implicit val createPostCommandReads = Json.reads[CreatePostCommand]
}