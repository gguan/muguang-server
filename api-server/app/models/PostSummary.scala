package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }

case class PostSummary(
  id: String,
  image: String,
  count: Int)

object PostSummary {

  implicit val postSummaryFormat: Format[PostSummary] = (
    (JsPath \ "id").format[String] and
    (JsPath \ "img").format[String] and
    (JsPath \ "c").format[Int]
  )(PostSummary.apply, unlift(PostSummary.unapply))
}