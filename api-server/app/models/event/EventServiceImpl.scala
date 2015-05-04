package models.event

import javax.inject.Inject

import models.{ User, Post, Event }
import reactivemongo.bson.BSONObjectID
import services.EventService

import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

class EventServiceImpl @Inject() (eventDAO: EventDAO) extends EventService {

  override def commentEvent(from: BSONObjectID, to: BSONObjectID, post: Post, comment: String): Future[Unit] = {
    val event = Event(BSONObjectID.generate,
      from,
      to,
      1,
      postId = Some(post._id),
      thumbnail = Some(post.photos.head.thumbnail),
      comment = Some(comment)
    )
    eventDAO.insert(event).map {
      case Left(ex) => throw ex
      case Right(e) => ()
    }
  }

  override def emotionEvent(from: BSONObjectID, to: BSONObjectID, post: Post, emotion: String): Future[Unit] = {
    val event = Event(BSONObjectID.generate,
      from,
      to,
      2,
      postId = Some(post._id),
      thumbnail = Some(post.photos.head.thumbnail),
      emotion = Some(emotion)
    )
    eventDAO.insert(event).map {
      case Left(ex) => throw ex
      case Right(e) => ()
    }
  }

  override def followEvent(from: BSONObjectID, to: BSONObjectID, user: User): Future[Unit] = {
    val event = Event(BSONObjectID.generate,
      from,
      to,
      3,
      thumbnail = user.avatarUrl
    )
    eventDAO.insert(event).map {
      case Left(ex) => throw ex
      case Right(e) => ()
    }
  }

}
