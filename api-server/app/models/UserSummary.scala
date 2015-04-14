package models

import play.api.libs.json.Json

case class UserSummary(id: String,
  username: String,
  avatar_url: Option[String],
  biography: Option[String],
  posts: Int,
  following: Int,
  followers: Int)

object UserSummary {
  implicit val userSummaryFormat = Json.format[UserSummary]
}
