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

  override def save(post: Post): Future[Post] = {
    insert(post).map {
      result =>
        result match {
          case Right(document) => document
          case Left(ex) => throw ex
        }
    }
  }

  override def delete(postId: String): Future[Boolean] = {
    remove(postId).map {
      result =>
        result match {
          case Right(b) => b
          case Left(ex) => throw ex
        }
    }
  }

}
