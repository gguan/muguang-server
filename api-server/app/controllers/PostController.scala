package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, Logger, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.Action
import play.extras.geojson._
import reactivemongo.bson.BSONObjectID
import services.{ TimelineService, PostService }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PostController @Inject() (
  implicit val env: Environment[User, JWTAuthenticator],
  val postService: PostService,
  val feedService: TimelineService) extends Silhouette[User, JWTAuthenticator] with Logger {

  def getPost(postId: String) = SecuredAction.async { implicit request =>
    postService.getPostById(BSONObjectID(postId)).map(postOpt => postOpt match {
      case Some(post) => Ok(Json.toJson(post))
      case None => NotFound
    })
  }

  def publishPost() = SecuredAction.async(parse.json) { implicit request =>
    println(request.body.as[CreatePostCommand])
    request.body.validate[CreatePostCommand].asOpt match {
      case Some(postCommand) => {
        println(postCommand)
        val post = postService.validatePostCommand(postCommand, request.identity)
        println(post)
        postService.publishPost(request.identity, post).map(p => Ok(Json.obj("id" -> post._id.stringify)))
        // TODO also publish to timeline cache
      }
      case None => Future.successful(BadRequest)
    }
  }

  def deletePost(postId: String) = SecuredAction.async { implicit request =>
    postService.deletePost(BSONObjectID(postId), request.identity).map(_ => Ok)
  }

  def getPostFor(userId: String, limit: Int, anchor: Option[String]) = SecuredAction.async { implicit request =>
    postService.getPostFor(BSONObjectID(userId), limit, anchor.map(BSONObjectID(_))).map {
      list =>
        val summaries = list.map { p =>
          PostSummary(p._id.stringify, p.photos.headOption.map(_.thumbnail).getOrElse(""), p.photos.size)
        }
        Ok(Json.toJson(summaries))
    }
  }

  def commentPost(postId: String) = SecuredAction.async(parse.json) { implicit request =>

    val replyTo = (request.body \ "reply_to").asOpt[String].map(BSONObjectID(_))
    val body = (request.body \ "body").as[String]
    val location = (request.body \ "location").asOpt[Feature[LatLng]]

    val comment = Comment(BSONObjectID.generate, request.identity._id, replyTo, body, DateTime.now, location)

    postService.commentPost(postId, comment).map(result => Ok)
  }

  def deleteComment(postId: String, commentId: String) = SecuredAction.async { implicit request =>
    postService.deleteCommentByUser(postId, commentId, request.identity).map { result =>
      if (result) Ok
      else BadRequest
    }
  }

  def likePost(postId: String) = SecuredAction.async(parse.json) { implicit request =>

    val code = (request.body \ "code").as[String]

    val emotion = PostEmotion(request.identity._id, code)

    postService.likePost(postId, emotion).map(result => Ok)
  }

  def unlikePost(postId: String) = SecuredAction.async(parse.json) { implicit request =>

    postService.unlikePost(postId, request.identity).map(result => Ok)
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
