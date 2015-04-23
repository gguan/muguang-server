package models

import play.api.libs.json.Json

case class UserSummary(id: String,
  screenName: String,
  avatar_url: Option[String],
  biography: Option[String],
  location: Option[String],
  posts: Option[Int] = None,
  following: Option[Int] = None,
  followers: Option[Int] = None)

object UserSummary {
  implicit val userSummaryFormat = Json.format[UserSummary]
}
