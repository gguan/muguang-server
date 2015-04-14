package com.muguang.core.db

import play.api.Play.current
import play.modules.reactivemongo.ReactiveMongoPlugin

import reactivemongo.bson.{ BSONObjectID, BSONValue }

import com.muguang.core.helpers.ContextHelper

trait MongoHelper extends ContextHelper {
  lazy val db = ReactiveMongoPlugin.db
}

object MongoHelper extends MongoHelper {
  def identify(bson: BSONValue) = bson.asInstanceOf[BSONObjectID].stringify
}
