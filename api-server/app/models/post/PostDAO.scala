package models.post

import com.muguang.core.dao.DocumentDao
import models.Post
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future

trait PostDAO extends DocumentDao[Post] {

  def getCollectionName(): String

  def runCommand(command: BSONDocument): Future[BSONDocument]

}
