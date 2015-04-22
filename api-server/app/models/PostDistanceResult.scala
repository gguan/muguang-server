package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }

case class PostDistanceResult(
  distance: Double,
  post: Post)

object PostDistanceResult {
  implicit val postDistanceResultFormat: Format[PostDistanceResult] = (
    (JsPath \ "dis").format[Double] and
    (JsPath \ "obj").format[Post]
  )(PostDistanceResult.apply, unlift(PostDistanceResult.unapply))
}