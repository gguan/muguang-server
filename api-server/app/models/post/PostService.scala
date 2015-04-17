package models.post

import models.{ User, CreatePostCommand, Post }

import scala.concurrent.Future

trait PostService {

  def save(post: Post): Future[Post]

  def delete(postId: String, user: User): Future[Boolean]

  def createPost(postCommand: CreatePostCommand, user: User): Post

}
