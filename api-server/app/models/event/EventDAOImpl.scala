package models.event

import com.muguang.core.dao.BaseDocumentDao
import models.Event
import reactivemongo.api.indexes.IndexType

import scala.concurrent.Future

class EventDAOImpl extends EventDAO with BaseDocumentDao[Event] {

  override val collectionName: String = "events"

  override def ensureIndexes: Future[List[Boolean]] = {
    for {
      eventIndex <- ensureIndex(List(("_f", IndexType.Ascending), ("_t", IndexType.Ascending)))
    } yield {
      List(eventIndex)
    }
  }

}
