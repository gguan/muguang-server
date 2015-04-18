package models.post

import com.muguang.core.dao.BaseDocumentDao
import models.Post
import reactivemongo.api.indexes.IndexType

import scala.concurrent.Future

class PostDAOImpl extends PostDAO with BaseDocumentDao[Post] {

  override val collectionName: String = "posts"

  override def ensureIndexes: Future[List[Boolean]] = {
    for {
      userIdIndex <- ensureIndex(List(("uid" -> IndexType.Ascending)))
      locationIndex <- ensureIndex(List(("loc" -> IndexType.Geo2DSpherical)))
    } yield {
      List(userIdIndex, locationIndex)
    }
  }

}