package models

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
  status: Option[String],
  created: DateTime = DateTime.now,
  comments: Seq[Comment] = Seq(),
  location: Option[Feature[LatLng]] = None,
  altitude: Option[Double] = None,
  emotions: Seq[PostEmotion] = Seq(),
  hashtags: Set[String] = Set(),
  flag: Option[String] = None,
  score: Option[Double] = None) extends IdentifiableModel {

  def toFeedCache(): FeedCacheItem = {
    FeedCacheItem(_id, userId, `type`)
  }
}

object Post {

  import play.modules.reactivemongo.json.BSONFormats._

  implicit val postFormat: Format[Post] = (
    (JsPath \ "_id").format[BSONObjectID] and
    (JsPath \ "_u").format[BSONObjectID] and
    (JsPath \ "_t").format[String] and
    (JsPath \ "pt").format[Seq[PostPhoto]] and
    (JsPath \ "s").formatNullable[String] and
    (JsPath \ "ct").format[DateTime] and
    (JsPath \ "cm").format[Seq[Comment]] and
    (JsPath \ "loc").formatNullable[Feature[LatLng]] and
    (JsPath \ "alt").formatNullable[Double] and
    (JsPath \ "emt").format[Seq[PostEmotion]] and
    (JsPath \ "tag").format[Set[String]] and
    (JsPath \ "flg").formatNullable[String] and
    (JsPath \ "sc").formatNullable[Double]
  )(Post.apply, unlift(Post.unapply))

}