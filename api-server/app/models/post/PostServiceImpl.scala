package models.post

import javax.inject.Inject

import com.muguang.core.exceptions.ResourceNotFoundException
import com.muguang.util.ExtractUtils
import models._
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats._

import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PostServiceImpl @Inject() (postDAO: PostDAO) extends PostService {

  override def save(post: Post): Future[Post] = {
    postDAO.insert(post).map {
      result =>
        result match {
          case Right(document) => document
          case Left(ex) => throw ex
        }
    }
  }

  override def deleteByUser(postId: String, user: User): Future[Boolean] = {
    postDAO.findById(postId).flatMap(postOpt => postOpt match {
      case Some(post) => {
        if (post.userId == user._id) {
          postDAO.remove(postId).map {
            result =>
              result match {
                case Right(b) => b
                case Left(ex) => throw ex
              }
          }
        } else {
          Future.successful(false)
        }
      }
      case None => Future.successful(false)
    })
  }

  override def createPost(postCommand: CreatePostCommand, user: User): Post = {
    Post(
      BSONObjectID.generate,
      user._id,
      postCommand.`type`,
      postCommand.photos,
      postCommand.status,
      location = postCommand.location,
      altitude = postCommand.altitude,
      hashtags = ExtractUtils.extractHashtags(postCommand.status.getOrElse(""))
    )
  }

  override def commentPost(postId: String, comment: Comment): Future[Comment] = {
    val query = Json.obj("$push" -> Json.obj("cm" -> Json.toJson(comment)))
    postDAO.update(postId, query).map {
      result =>
        result match {
          case Right(b) => comment
          case Left(ex) => throw ex
        }
    }
  }

  override def deleteCommentByUser(postId: String, commentId: String, user: User): Future[Boolean] = {
    import play.modules.reactivemongo.json.BSONFormats._

    postDAO.findById(postId).flatMap(postOpt => postOpt match {
      case Some(post) => {
        val comment = post.comments.find(_._id.stringify == commentId)

        if (post.userId == user._id || comment.map(_.author) == Some(user._id)) {
          val query = Json.obj("$pull" -> Json.obj("cm" -> Json.obj("_id" -> Json.toJson(BSONObjectID(commentId)))))
          postDAO.update(postId, query).map {
            result =>
              result match {
                case Right(b) => true
                case Left(ex) => false
              }
          }
        } else {
          Future.successful(false)
        }
      }
      case None => Future.successful(false)
    })
  }

  override def likePost(postId: String, emotion: PostEmotion): Future[PostEmotion] = {
    postDAO.findById(postId).flatMap(postOpt => postOpt match {
      case Some(post) => {
        if (post.emotions.exists(_.userId == emotion.userId)) {
          val query = Json.obj("$set" -> Json.obj("em.$" -> Json.toJson(emotion)))
          postDAO.update(postId, query).map {
            result =>
              result match {
                case Right(b) => emotion
                case Left(ex) => throw ex
              }
          }
        } else {
          val query = Json.obj("$pull" -> Json.obj("em" -> Json.toJson(emotion)))
          postDAO.update(postId, query).map {
            result =>
              result match {
                case Right(b) => emotion
                case Left(ex) => throw ex
              }
          }
        }
      }
      case None => throw ResourceNotFoundException(postId)
    })
  }

  override def unlikePost(postId: String, user: User): Future[Boolean] = {
    val query = Json.obj("$pull" ->
      Json.obj("em" -> Json.obj("u" -> Json.toJson(user._id)))
    )
    postDAO.update(postId, query).map {
      result =>
        result match {
          case Right(b) => true
          case Left(ex) => throw ex
        }
    }
  }
}
