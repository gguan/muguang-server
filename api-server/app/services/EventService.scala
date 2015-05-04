package services

import models.{ User, Post }
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

trait EventService {

  def commentEvent(from: BSONObjectID, to: BSONObjectID, post: Post, comment: String): Future[Unit]

  def emotionEvent(from: BSONObjectID, to: BSONObjectID, post: Post, emotion: String): Future[Unit]

  def followEvent(from: BSONObjectID, to: BSONObjectID, user: User): Future[Unit]

}
