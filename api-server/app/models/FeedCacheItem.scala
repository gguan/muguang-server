package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }
import reactivemongo.bson.BSONObjectID

case class FeedCacheItem(
  _id: BSONObjectID,
  author: BSONObjectID,
  `type`: String)

object FeedCacheItem {

  import play.modules.reactivemongo.json.BSONFormats._

  implicit val feedFormat: Format[FeedCacheItem] = (
    (JsPath \ "_id").format[BSONObjectID] and
    (JsPath \ "_a").format[BSONObjectID] and
    (JsPath \ "_t").format[String]
  )(FeedCacheItem.apply, unlift(FeedCacheItem.unapply))
}