package models

import com.muguang.core.models.IdentifiableModel
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }
import reactivemongo.bson.BSONObjectID

case class TimelineCache(
  override var _id: BSONObjectID,
  feeds: List[FeedCacheItem],
  posts: List[FeedCacheItem]) extends IdentifiableModel

object TimelineCache {
  import play.modules.reactivemongo.json.BSONFormats._

  implicit val postFormat: Format[TimelineCache] = (
    (JsPath \ "_id").format[BSONObjectID] and
    (JsPath \ "_c").format[List[FeedCacheItem]] and
    (JsPath \ "_p").format[List[FeedCacheItem]]
  )(TimelineCache.apply, unlift(TimelineCache.unapply))
}