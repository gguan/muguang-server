package models

import com.mohiva.play.silhouette.api.Identity
import com.muguang.core.models.IdentifiableModel
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }
import play.extras.geojson.{ LatLng, Feature }
import reactivemongo.bson.BSONObjectID

case class Post(
  override var _id: BSONObjectID,
  userId: BSONObjectID,
  `type`: String,
  photos: Seq[PostPhoto],
  created: DateTime = DateTime.now,
  comments: Seq[Comment],
  location: Option[Feature[LatLng]],
  altitude: Option[Double],
  emotions: Seq[PostEmotion],
  hashtags: Set[String]) extends Identity with IdentifiableModel

object Post {

  import play.modules.reactivemongo.json.BSONFormats._

  implicit val postFormat: Format[Post] = (
    (JsPath \ "_id").format[BSONObjectID] and
    (JsPath \ "uid").format[BSONObjectID] and
    (JsPath \ "t").format[String] and
    (JsPath \ "pt").format[Seq[PostPhoto]] and
    (JsPath \ "ct").format[DateTime] and
    (JsPath \ "cm").format[Seq[Comment]] and
    (JsPath \ "loc").formatNullable[Feature[LatLng]] and
    (JsPath \ "alt").formatNullable[Double] and
    (JsPath \ "em").format[Seq[PostEmotion]] and
    (JsPath \ "ht").format[Set[String]]
  )(Post.apply, unlift(Post.unapply))

}