package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, Logger, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models._
import play.api.libs.json._
import play.api.mvc.Action
import play.extras.geojson._
import reactivemongo.bson.BSONObjectID
import services.{ EventService, TimelineService, PostService }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PostController @Inject() (
    implicit val env: Environment[User, JWTAuthenticator],
    val postService: PostService,
    val eventService: EventService,
    val timelineService: TimelineService) extends Silhouette[User, JWTAuthenticator] with Logger {

  def getPost(postId: String) = SecuredAction.async { implicit request =>
    postService.getPostById(BSONObjectID(postId)).map {
      case Some(post) => Ok(Json.toJson(post))
      case None       => NotFound
    }
  }

  def publishPost() = SecuredAction.async(parse.json) { implicit request =>
    request.body.validate[CreatePostCommand].asOpt match {
      case Some(postCommand) =>
        val post = postService.validatePostCommand(postCommand, request.identity)
        for {
          // push to post service
          post <- postService.publishPost(request.identity, post)
          // also push to timeline cache
          feed <- timelineService.feed(request.identity, post)
        } yield {
          Ok(Json.obj("id" -> post._id.stringify))
        }
      case None => Future.successful(BadRequest)
    }
  }

  def deletePost(postId: String) = SecuredAction.async { implicit request =>
    postService.deletePost(BSONObjectID(postId), request.identity).map(_ => Ok)
  }

  def getPostFor(userId: String, limit: Int, anchor: Option[String]) = SecuredAction.async { implicit request =>
    postService.getPostFor(BSONObjectID(userId), limit, anchor.map(BSONObjectID(_))).map { list =>
      val summaries = list.map { p =>
        PostSummary(p._id.stringify, p.photos.headOption.map(_.thumbnail).getOrElse(""), p.photos.size)
      }
      Ok(Json.toJson(summaries))
    }
  }

  def commentPost(postId: String) = SecuredAction.async(parse.json) { implicit request =>
    request.body.validate[CreateCommentCommand].asOpt match {
      case Some(commentCommand) =>
        val comment = Comment(
          BSONObjectID.generate,
          request.identity._id,
          commentCommand.body,
          replyTo = commentCommand.replyTo.map(BSONObjectID(_)),
          location = commentCommand.location
        )

        for {
          post <- postService.validatePost(postId)
          result <- postService.commentPost(post._id, comment, request.identity)
          event <- eventService.commentEvent(request.identity._id, post.userId, post, comment.body)
        } yield {
          Ok
        }

      case None => Future.successful(BadRequest)
    }
  }

  def deleteComment(postId: String, commentId: String) = SecuredAction.async { implicit request =>
    postService.deleteComment(BSONObjectID(postId), BSONObjectID(commentId), request.identity).map(_ => Ok)
  }

  def likePost(postId: String) = SecuredAction.async(parse.json) { implicit request =>
    request.body.validate[CreateEmotionCommand].asOpt match {
      case Some(emotionCommand) =>
        val emotion = PostEmotion(request.identity._id, emotionCommand.code)
        for {
          post <- postService.validatePost(postId)
          result <- postService.likePost(BSONObjectID(postId), emotion, request.identity)
          event <- eventService.emotionEvent(request.identity._id, post.userId, post, emotion.code)
        } yield {
          Ok
        }

      case None => Future.successful(BadRequest)
    }
  }

  def unlikePost(postId: String) = SecuredAction.async { implicit request =>
    postService.unlikePost(BSONObjectID(postId), request.identity).map(result => Ok)
  }

  def getTimelineFor(userId: String, limit: Int, anchor: Option[String]) = SecuredAction.async { implicit request =>
    timelineService
      .getFeedFor(BSONObjectID(userId), limit, anchor.map(BSONObjectID(_)))
      .map { feeds => Ok(Json.toJson(feeds)) }
  }

  def getOneRandomPost() = Action.async { implicit request =>
    postService.getOneRandomPost.map(p => Ok(Json.toJson(p)))
  }

  def searchNearbyPosts() = Action.async(parse.json) { implicit request =>
    val latLng = (request.body \ "coordinates").as[LatLng]
    val maxDistance = (request.body \ "max_distance").asOpt[Int]
    val minDistance = (request.body \ "min_distance").asOpt[Int]
    val query = (request.body \ "query").asOpt[JsObject]

    postService.searchNearbyPosts(latLng, maxDistance, minDistance, query).map { list =>
      list.foreach(p => println(Json.toJson(p)))
      Ok(Json.toJson(list))
    }
  }

}
