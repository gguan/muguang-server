package models

import play.api.libs.json.Json
import play.extras.geojson.LatLng

case class SearchPostCommand(
  center: LatLng,
  minDistance: Int = 0,
  maxDistance: Int = 2000,
  limit: Int = 100,
  bearings: Option[Double] = None)

object SearchPostCommand {
  implicit val searchPostCommandReads = Json.format[SearchPostCommand]

}
