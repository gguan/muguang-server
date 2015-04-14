package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }
import play.extras.geojson._
import reactivemongo.bson.BSONObjectID

case class PhotoTag(
  `type`: Int,
  name: String,
  x: Double,
  y: Double,
  location: Option[Feature[LatLng]],
  people: Option[BSONObjectID])

object PhotoTag {
  import play.modules.reactivemongo.json.BSONFormats._

  implicit val photoTagFormat: Format[PhotoTag] = (
    (JsPath \ "t").format[Int] and
    (JsPath \ "n").format[String] and
    (JsPath \ "px").format[Double] and
    (JsPath \ "py").format[Double] and
    (JsPath \ "l").formatNullable[Feature[LatLng]] and
    (JsPath \ "p").formatNullable[BSONObjectID]
  )(PhotoTag.apply, unlift(PhotoTag.unapply))
}