package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }

case class PostPhoto(
  thumbnail: String,
  url: String,
  tags: Seq[PhotoTag])

object PostPhoto {

  implicit val postPhotoFormat: Format[PostPhoto] = (
    (JsPath \ "t").format[String] and
    (JsPath \ "i").format[String] and
    (JsPath \ "t").format[Seq[PhotoTag]]
  )(PostPhoto.apply, unlift(PostPhoto.unapply))

}