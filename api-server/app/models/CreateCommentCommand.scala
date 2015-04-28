package models

import play.api.libs.json._
import play.extras.geojson.{ LatLng, Feature }

case class CreateCommentCommand(
  body: String,
  replyTo: Option[String],
  location: Option[Feature[LatLng]])

object CreateCommentCommand {
  implicit val createCommentCommandReads = Json.reads[CreateCommentCommand]
}