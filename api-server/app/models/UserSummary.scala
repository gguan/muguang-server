package models

import play.api.libs.json.Json

case class UserSummary(
  id: Option[String] = None,
  screen_name: String,
  avatar_url: Option[String] = None,
  biography: Option[String] = None,
  location: Option[String] = None,
  posts: Option[Int] = None,
  following: Option[Int] = None,
  followers: Option[Int] = None)

object UserSummary {
  implicit val userSummaryFormat = Json.format[UserSummary]
}
