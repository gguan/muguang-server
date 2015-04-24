package com.muguang.core.dao

import scala.concurrent.Future

import play.api.Logger

import reactivemongo.core.commands.LastError
import reactivemongo.core.errors.DatabaseException

import com.muguang.core.db.MongoHelper
import com.muguang.core.exceptions._

trait BaseDao extends MongoHelper {

  val collectionName: String

  def ensureIndexes: Future[List[Boolean]]

}
