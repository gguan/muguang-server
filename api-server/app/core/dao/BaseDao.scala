package com.muguang.core.dao

import scala.concurrent.Future

import com.muguang.core.db.MongoHelper

trait BaseDao extends MongoHelper {

  val collectionName: String

  def ensureIndexes: Future[List[Boolean]]

}
