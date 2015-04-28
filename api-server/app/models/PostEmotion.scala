package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }
import reactivemongo.bson.BSONObjectID

case class PostEmotion(
  userId: BSONObjectID,
  code: String)

object PostEmotion {
  import play.modules.reactivemongo.json.BSONFormats._

  implicit val postEmotionFormat: Format[PostEmotion] = (
    (JsPath \ "_u").format[BSONObjectID] and
    (JsPath \ "_c").format[String]
  )(PostEmotion.apply, unlift(PostEmotion.unapply))

}