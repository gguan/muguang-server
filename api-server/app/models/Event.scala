package models

import com.muguang.core.models.IdentifiableModel
import org.joda.time.DateTime
import play.api.libs.json.{ JsPath, Format }
import play.api.libs.functional.syntax._
import reactivemongo.bson.BSONObjectID

// type
// 1. comment
// 2. emotion
// 3. friend request
case class Event(
  override var _id: BSONObjectID,
  from: BSONObjectID,
  to: BSONObjectID,
  `type`: Int,
  timestamp: DateTime = DateTime.now,
  postId: Option[BSONObjectID] = None,
  thumbnail: Option[String] = None,
  comment: Option[String] = None,
  emotion: Option[String] = None) extends IdentifiableModel

object Event {
  import play.modules.reactivemongo.json.BSONFormats._

  implicit val eventFormat: Format[Event] = (
    (JsPath \ "_id").format[BSONObjectID] and
    (JsPath \ "_f").format[BSONObjectID] and
    (JsPath \ "_t").format[BSONObjectID] and
    (JsPath \ "_e").format[Int] and
    (JsPath \ "ct").format[DateTime] and
    (JsPath \ "pid").formatNullable[BSONObjectID] and
    (JsPath \ "tb").formatNullable[String] and
    (JsPath \ "cm").formatNullable[String] and
    (JsPath \ "em").formatNullable[String]
  )(Event.apply, unlift(Event.unapply))
}
