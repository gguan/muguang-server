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
  screenName: String,
  refreshToken: Option[String],
  phone: Option[String] = None,
  email: Option[String] = None,
  created: DateTime = DateTime.now,
  biography: Option[String] = None,
  location: Option[String] = None,
  gender: Option[String] = None,
  avatarUrl: Option[String] = None,
  postCount: Int = 0,
  followingCount: Int = 0,
  followersCount: Int = 0) extends Identity with IdentifiableModel

object User {
  import play.modules.reactivemongo.json.BSONFormats._

  implicit val loginInfoFormat: Format[LoginInfo] = (
    (JsPath \ "pid").format[String] and
    (JsPath \ "pk").format[String]
  )(LoginInfo.apply, unlift(LoginInfo.unapply))

  implicit val userFormat: Format[User] = (
    (JsPath \ "_id").format[BSONObjectID] and
    (JsPath \ "li").format[LoginInfo] and
    (JsPath \ "sn").format[String] and
    (JsPath \ "rt").formatNullable[String] and
    (JsPath \ "pho").formatNullable[String] and
    (JsPath \ "e").formatNullable[String] and
    (JsPath \ "ct").format[DateTime] and
    (JsPath \ "bio").formatNullable[String] and
    (JsPath \ "loc").formatNullable[String] and
    (JsPath \ "gen").formatNullable[String] and
    (JsPath \ "avt").formatNullable[String] and
    (JsPath \ "_cp").format[Int] and
    (JsPath \ "_cfg").format[Int] and
    (JsPath \ "_cfr").format[Int]
  )(User.apply, unlift(User.unapply))

}

