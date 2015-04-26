package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }

case class PostPhoto(
  thumbnail: String,
  url: String)

object PostPhoto {

  implicit val postPhotoFormat: Format[PostPhoto] = (
    (JsPath \ "_s").format[String] and
    (JsPath \ "_i").format[String]
  )(PostPhoto.apply, unlift(PostPhoto.unapply))

}