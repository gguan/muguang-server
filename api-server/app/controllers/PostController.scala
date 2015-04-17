package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, Logger, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import models.post.PostService
import models.{ CreatePostCommand, Post, User }
import models.user.UserService
import play.api.libs.json.Json

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

  def deletePost(postId: String) = SecuredAction.async(parse.json) { implicit request =>
    postService.delete(postId, request.identity).map { result =>
      if (result) Ok
      else BadRequest
    }
  }

}
