package models

import play.api.libs.json._

case class CreateEmotionCommand(code: String)

object CreateEmotionCommand {
  implicit val createEmotionCommandReads = Json.reads[CreateEmotionCommand]
}