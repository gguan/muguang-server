package models.post

import com.muguang.core.dao.BaseDocumentDao
import models.Post
import play.api.libs.json.JsObject
import play.extras.geojson.LatLng
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.{ BSONArray, BSONDocument }
import reactivemongo.core.commands.RawCommand

import scala.concurrent.Future

class PostDAOImpl extends PostDAO with BaseDocumentDao[Post] {

  override val collectionName: String = "posts"

  override def ensureIndexes: Future[List[Boolean]] = {
    for {
      userIdIndex <- ensureIndex(List(("uid" -> IndexType.Ascending)))
      locationIndex <- ensureIndex(List(("loc.geometry.coordinates" -> IndexType.Geo2DSpherical)))
    } yield {
      List(userIdIndex, locationIndex)
    }
  }

  def runCommand(command: BSONDocument): Future[BSONDocument] = {
    db.command(RawCommand(command))
  }

  def getCollectionName(): String = {
    collectionName
  }

}
