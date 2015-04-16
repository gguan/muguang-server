package models

import com.muguang.core.models.IdentifiableModel
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import org.joda.time.DateTime
import play.api.libs.json.JsPath
import reactivemongo.bson.BSONObjectID

case class User(
  override var _id: BSONObjectID,
  loginInfo: LoginInfo,
  refreshToken: Option[String],
  screenName: String,
  email: Option[String] = None,
  created: DateTime = DateTime.now,
  biography: Option[String] = None,
  location: Option[String] = None,
  gender: Option[String] = None,
  avatarUrl: Option[String] = None,
  following: Seq[String] = Seq(),
  followingCounts: Int = 0,
  followerCounts: Int = 0) extends Identity with IdentifiableModel

object User {
  import play.modules.reactivemongo.json.BSONFormats._

  implicit val loginInfoFormat: Format[LoginInfo] = (
    (JsPath \ "pid").format[String] and
    (JsPath \ "pk").format[String]
  )(LoginInfo.apply, unlift(LoginInfo.unapply))

  implicit val userFormat: Format[User] = (
    (JsPath \ "_id").format[BSONObjectID] and
    (JsPath \ "li").format[LoginInfo] and
    (JsPath \ "rt").formatNullable[String] and
    (JsPath \ "sn").format[String] and
    (JsPath \ "e").formatNullable[String] and
    (JsPath \ "ct").format[DateTime] and
    (JsPath \ "b").formatNullable[String] and
    (JsPath \ "l").formatNullable[String] and
    (JsPath \ "g").formatNullable[String] and
    (JsPath \ "a").formatNullable[String] and
    (JsPath \ "f").format[Seq[String]] and
    (JsPath \ "fwc").format[Int] and
    (JsPath \ "frc").format[Int]
  )(User.apply, unlift(User.unapply))

}

