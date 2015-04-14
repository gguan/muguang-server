package com.muguang.core.models

import reactivemongo.bson.BSONObjectID

trait IdentifiableModel {

  var _id: BSONObjectID

  def identify = _id.stringify
}
