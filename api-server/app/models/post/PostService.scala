package models.post

import models.{ Comment, User, CreatePostCommand, Post }

import scala.concurrent.Future

trait PostService {

  def save(post: Post): Future[Post]

  def deleteByUser(postId: String, user: User): Future[Boolean]

  def createPost(postCommand: CreatePostCommand, user: User): Post

  def addComment(postId: String, comment: Comment): Future[Comment]

  def deleteCommentByUser(postId: String, commentId: String, user: User): Future[Boolean]

}
