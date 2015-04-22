package models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Format }

case class RefreshToken(token: String, expireDate: DateTime) {
  def isValid(): Boolean = expireDate.isAfterNow
}

object RefreshToken {
  implicit val refreshToken: Format[RefreshToken] = (
    (JsPath \ "t").format[String] and
    (JsPath \ "exp").format[DateTime]
  )(RefreshToken.apply, unlift(RefreshToken.unapply))
}