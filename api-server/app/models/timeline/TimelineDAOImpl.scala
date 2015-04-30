package models.timeline

import com.muguang.core.dao.BaseDocumentDao
import models.TimelineCache

import scala.concurrent.Future

class TimelineDAOImpl extends TimelineDAO with BaseDocumentDao[TimelineCache] {

  override val collectionName: String = "timeline_cache"

  override def ensureIndexes: Future[List[Boolean]] = Future.successful(Nil)

}
