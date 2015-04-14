package models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }
import reactivemongo.bson.BSONObjectID
import play.extras.geojson._

case class Comment(
  author: BSONObjectID,
  replyTo: Option[BSONObjectID],
  body: String,
  created: DateTime,
  location: Feature[LatLng])

object Comment {
  import play.modules.reactivemongo.json.BSONFormats._

  implicit val commentFormat: Format[Comment] = (
    (JsPath \ "u").format[BSONObjectID] and
    (JsPath \ "r").formatNullable[BSONObjectID] and
    (JsPath \ "b").format[String] and
    (JsPath \ "ct").format[DateTime] and
    (JsPath \ "l").format[Feature[LatLng]]
  )(Comment.apply, unlift(Comment.unapply))

}