package com.muguang.core.models

import reactivemongo.bson.BSONObjectID

trait IdentifiableModel {

  var _id: Option[BSONObjectID]

  def identify = _id.map(value => value.stringify).getOrElse("unknown")
}
