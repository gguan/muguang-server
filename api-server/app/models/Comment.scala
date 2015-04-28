package models

import com.muguang.core.models.IdentifiableModel
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }
import reactivemongo.bson.BSONObjectID
import play.extras.geojson._

case class Comment(
  override var _id: BSONObjectID,
  author: BSONObjectID,
  body: String,
  created: DateTime = DateTime.now,
  replyTo: Option[BSONObjectID],
  location: Option[Feature[LatLng]]) extends IdentifiableModel

object Comment {
  import play.modules.reactivemongo.json.BSONFormats._

  implicit val commentFormat: Format[Comment] = (
    (JsPath \ "_id").format[BSONObjectID] and
    (JsPath \ "_u").format[BSONObjectID] and
    (JsPath \ "_b").format[String] and
    (JsPath \ "_d").format[DateTime] and
    (JsPath \ "r").formatNullable[BSONObjectID] and
    (JsPath \ "l").formatNullable[Feature[LatLng]]
  )(Comment.apply, unlift(Comment.unapply))

}