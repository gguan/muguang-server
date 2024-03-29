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
  created: DateTime = DateTime.now,
  photos: Seq[PostPhoto],
  status: Option[String],
  anonymous: Option[Boolean] = Some(false),
  comments: Seq[Comment] = Seq(),
  emotions: Seq[PostEmotion] = Seq(),
  location: Option[Feature[LatLng]] = None,
  altitude: Option[Double] = None,
  hashtags: Set[String] = Set(),
  flag: Option[String] = None,
  score: Option[Double] = None) extends IdentifiableModel {

  def toFeedCache: FeedCacheItem = {
    FeedCacheItem(_id, userId, `type`)
  }
}

object Post {

  import play.modules.reactivemongo.json.BSONFormats._

  implicit val postFormat: Format[Post] = (
    (JsPath \ "_id").format[BSONObjectID] and
    (JsPath \ "_u").format[BSONObjectID] and
    (JsPath \ "_t").format[String] and
    (JsPath \ "_d").format[DateTime] and
    (JsPath \ "_p").format[Seq[PostPhoto]] and
    (JsPath \ "_b").formatNullable[String] and
    (JsPath \ "_a").formatNullable[Boolean] and
    (JsPath \ "cm").format[Seq[Comment]] and
    (JsPath \ "em").format[Seq[PostEmotion]] and
    (JsPath \ "loc").formatNullable[Feature[LatLng]] and
    (JsPath \ "alt").formatNullable[Double] and
    (JsPath \ "tag").format[Set[String]] and
    (JsPath \ "flg").formatNullable[String] and
    (JsPath \ "sc").formatNullable[Double]
  )(Post.apply, unlift(Post.unapply))

}