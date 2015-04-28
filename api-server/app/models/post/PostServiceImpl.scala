package models.post

import javax.inject.Inject

import com.muguang.core.exceptions.{ OperationNotAllowedException, InvalidResourceException, ResourceNotFoundException }
import com.muguang.util.ExtractUtils
import com.muguang.core.db.MongoHelper.HandleDBFailure
import models._
import models.user.UserDAO
import play.api.libs.json.{ JsObject, Json }
import play.extras.geojson.LatLng
import play.modules.reactivemongo.json.BSONFormats._

import reactivemongo.bson.{ BSONArray, BSONDocument, BSONObjectID }
import services.PostService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class PostServiceImpl @Inject() (postDAO: PostDAO, userDAO: UserDAO) extends PostService {

  val PostCollectionName = "posts"

  override def validatePostCommand(postCommand: CreatePostCommand, user: User): Post = {
    postCommand.`type` match {
      case "photo" =>
        Post(
          BSONObjectID.generate,
          user._id,
          postCommand.`type`,
          photos = postCommand.photos,
          status = postCommand.status,
          location = postCommand.location,
          altitude = postCommand.altitude,
          hashtags = ExtractUtils.extractHashtags(postCommand.status.getOrElse(""))
        )
      case _ => throw InvalidResourceException("Invalid post type")
    }
  }

  override def getPostById(postId: BSONObjectID): Future[Option[Post]] = postDAO.findById(postId)

  override def deletePost(postId: BSONObjectID, user: User): Future[Unit] = {
    postDAO.findById(postId).flatMap {
      case Some(post) =>
        if (post.userId == user._id) {
          for {
            result <- HandleDBFailure(postDAO.remove(postId))
            postCount <- HandleDBFailure(userDAO.update(user._id, Json.obj("$inc" -> Json.obj("_cp" -> -1))))
          } yield {}
        } else {
          throw OperationNotAllowedException(s"${user._id.stringify} delete post[${postId.stringify}]")
        }
      case None => throw ResourceNotFoundException(postId.stringify, s"post[${postId.stringify}]")
    }
  }

  override def publishPost(user: User, post: Post): Future[Post] = {
    for {
      result <- HandleDBFailure(postDAO.insert(post))
      postCount <- HandleDBFailure(userDAO.update(user._id, Json.obj("$inc" -> Json.obj("_cp" -> 1))))
    } yield {
      result
    }
  }

  override def countPostFor(userId: BSONObjectID): Future[Int] = {
    postDAO.count(Json.obj("_u" -> userId))
  }

  override def getPostFor(userId: BSONObjectID, limit: Int, anchor: Option[BSONObjectID]): Future[List[Post]] = {
    val query = if (anchor.isDefined) {
      Json.obj(
        "_u" -> userId,
        "_id" -> Json.obj("$lt" -> anchor.get)
      )
    } else {
      Json.obj("_u" -> userId)
    }
    postDAO.findWithLimit(query, limit)
  }

  override def getPostFor(users: Seq[BSONObjectID], limit: Int, anchor: Option[BSONObjectID]): Future[List[Post]] = {
    val query = if (anchor.isEmpty) {
      Json.obj("_u" -> Json.obj("$in" -> users))
    } else {
      Json.obj(
        "_u" -> Json.obj("$in" -> users),
        "_id" -> Json.obj("$lt" -> anchor.get)
      )
    }
    postDAO.findWithLimit(query, limit)
  }

  override def commentPost(postId: BSONObjectID, comment: Comment, user: User): Future[Unit] = {
    val query = Json.obj("$push" -> Json.obj("cm" -> Json.toJson(comment)))
    postDAO.update(postId, query).map {
      case Right(b) => ()
      case Left(ex) => throw ex
    }
  }

  override def deleteComment(postId: BSONObjectID, commentId: BSONObjectID, user: User): Future[Unit] = {
    postDAO.findById(postId).flatMap {
      case Some(post) =>
        val comment = post.comments.find(_._id.stringify == commentId)
        if (post.userId == user._id ||
          (comment.isDefined && comment.get.author == user._id)) {
          val query = Json.obj("$pull" -> Json.obj("cm" -> Json.obj("_id" -> commentId)))
          postDAO.update(postId, query).map {
            case Right(b) => ()
            case Left(ex) => throw ex
          }
        } else {
          throw OperationNotAllowedException(s"user[${user.identify}] delete comment ${commentId.stringify} on post[${postId.stringify}}]")
        }
      case None => throw ResourceNotFoundException("post:" + postId.stringify)
    }
  }

  override def likePost(postId: BSONObjectID, emotion: PostEmotion, user: User): Future[Unit] = {
    postDAO.findById(postId).flatMap {
      case Some(post) =>
        if (post.emotions.exists(_.userId == user._id)) {
          val value = Json.obj("_id" -> postId, "em._u" -> user._id)
          val query = Json.obj("$set" -> Json.obj("em.$" -> emotion))
          postDAO.update(value, query).map {
            case Right(b) => ()
            case Left(ex) => throw ex
          }
        } else {
          val query = Json.obj("$push" -> Json.obj("em" -> emotion))
          postDAO.update(postId, query).map {
            case Right(b) => ()
            case Left(ex) => throw ex
          }
        }
      case None => throw ResourceNotFoundException(postId.stringify)
    }
  }

  override def unlikePost(postId: BSONObjectID, user: User): Future[Unit] = {
    val query = Json.obj("$pull" ->
      Json.obj("em" ->
        Json.obj("_u" -> user._id)
      )
    )
    postDAO.update(postId, query).map {
      case Right(b) => ()
      case Left(ex) => throw ex
    }
  }

  override def getOneRandomPost: Future[Option[Post]] = {
    postDAO.count(Json.obj()).flatMap { number =>
      postDAO.findWithOptions(Json.obj(), Random.nextInt(number), 1).map(_.headOption)
    }
  }

  def searchNearbyPosts(latLng: LatLng, maxDistance: Option[Int] = None, minDistance: Option[Int] = None, query: Option[JsObject] = None): Future[List[PostDistanceResult]] = {
    import play.modules.reactivemongo.json.BSONFormats._

    val command = BSONDocument(
      "geoNear" -> PostCollectionName,
      "near" -> BSONDocument(
        "type" -> "Point",
        "coordinates" -> BSONArray(latLng.lng, latLng.lat)
      ),
      "spherical" -> true
    ) ++ (maxDistance match {
        case Some(max) => BSONDocument("maxDistance" -> max)
        case None => BSONDocument()
      }) ++ (minDistance match {
        case Some(min) => BSONDocument("minDistance" -> min)
        case None => BSONDocument()
      }) ++ (query match {
        case Some(q) => BSONDocument("query" -> q.as[BSONDocument])
        case None => BSONDocument()
      })

    postDAO.runCommand(command).map { doc =>
      (Json.toJson(doc) \ "results").as[List[PostDistanceResult]]
    }

  }

}
