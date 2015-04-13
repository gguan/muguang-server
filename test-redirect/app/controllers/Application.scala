package controllers

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

object Application extends Controller {

  def auth = Action { implicit request =>
    Logger.info("remote:\t" + request.remoteAddress)
    Logger.info("uri:\t" + request.uri)
    Ok(Json.obj(
      "remote" -> request.remoteAddress,
      "uri" -> request.uri,
      "uid" -> 123456,
      "access_token" -> "test_token"
    ))
  }

}