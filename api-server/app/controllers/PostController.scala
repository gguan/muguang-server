package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, Logger, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import models.post.PostService
import models._
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.Action
import play.extras.geojson._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PostController @Inject() (
  val env: Environment[User, BearerTokenAuthenticator],
  val postService: PostService) extends Silhouette[User, BearerTokenAuthenticator] with Logger {

  def createPost() = SecuredAction.async(parse.json) { implicit request =>
    request.body.validate[CreatePostCommand].asOpt match {
      case Some(postCommand) => {
        val post = postService.createPost(postCommand, request.identity)
        postService.save(post).map(p => Ok(Json.toJson(post)))
      }
      case None => Future.successful(BadRequest)
    }
  }

  def deletePost(postId: String) = SecuredAction.async { implicit request =>
    postService.deleteByUser(postId, request.identity).map { result =>
      if (result) Ok
      else BadRequest
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

    postService.getOneRandomPost().map(p => Ok(Json.toJson(p)))
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
