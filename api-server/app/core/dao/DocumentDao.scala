package com.muguang.core.dao

import com.muguang.core.exceptions.ServiceException
import com.muguang.core.models.IdentifiableModel
import play.api.libs.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.{ BSONDocument, BSONObjectID }

import scala.concurrent.Future

trait DocumentDao[M <: IdentifiableModel] {

  val collection: JSONCollection

  def insert(document: M)(implicit writer: Writes[M]): Future[Either[ServiceException, M]]

  def find(query: JsObject = Json.obj())(implicit reader: Reads[M]): Future[List[M]]

  def findWithLimit(query: JsObject = Json.obj(), limit: Int)(implicit reader: Reads[M]): Future[List[M]]

  def findWithFilter(query: JsObject = Json.obj(), filter: JsObject = Json.obj())(implicit reader: Reads[M]): Future[List[M]]

  def findWithOptions(query: JsObject = Json.obj(), skip: Int, limit: Int)(implicit reader: Reads[M]): Future[List[M]]

  def findWithFilterAndOptions(query: JsObject = Json.obj(), filter: JsObject = Json.obj(), skip: Int, limit: Int)(implicit reader: Reads[M]): Future[List[M]]

  def findById(id: String)(implicit reader: Reads[M]): Future[Option[M]]

  def findById(id: BSONObjectID)(implicit reader: Reads[M]): Future[Option[M]]

  def findOne(query: JsObject)(implicit reader: Reads[M]): Future[Option[M]]

  def update(id: String, document: M)(implicit writer: Writes[M]): Future[Either[ServiceException, M]]

  def update(id: String, query: JsObject): Future[Either[ServiceException, JsObject]]

  def update(id: BSONObjectID, query: JsObject): Future[Either[ServiceException, JsObject]]

  def update(value: JsObject, query: JsObject): Future[Either[ServiceException, JsObject]]

  def push[S](id: String, field: String, data: S)(implicit writer: Writes[S]): Future[Either[ServiceException, S]]

  def pull[S](id: String, field: String, query: S)(implicit writer: Writes[S]): Future[Either[ServiceException, Boolean]]

  def unset(id: String, field: String): Future[Either[ServiceException, Boolean]]

  def remove(id: String): Future[Either[ServiceException, Boolean]]

  def remove(id: BSONObjectID): Future[Either[ServiceException, Boolean]]

  def remove(query: JsObject, firstMatchOnly: Boolean = false): Future[Either[ServiceException, Boolean]]

  def updated(data: JsObject): JsObject

  def count(query: JsObject): Future[Int]

  def ensureIndex(
    key: List[(String, IndexType)],
    name: Option[String] = None,
    unique: Boolean = false,
    background: Boolean = false,
    dropDups: Boolean = false,
    sparse: Boolean = false,
    version: Option[Int] = None,
    options: BSONDocument = BSONDocument()): Future[Boolean]
}
