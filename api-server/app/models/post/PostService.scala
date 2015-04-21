package models.post

import models._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

trait PostService {

  def save(post: Post): Future[Post]

  def deleteByUser(postId: String, user: User): Future[Boolean]

  def createPost(postCommand: CreatePostCommand, user: User): Post

  def commentPost(postId: String, comment: Comment): Future[Comment]

  def deleteCommentByUser(postId: String, commentId: String, user: User): Future[Boolean]

  def likePost(postId: String, emotion: PostEmotion): Future[PostEmotion]

  def unlikePost(postId: String, user: User): Future[Boolean]

}
