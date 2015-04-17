package models.post

import javax.inject.Inject

import com.muguang.util.ExtractUtils
import models.{ User, CreatePostCommand, Post }
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PostServiceImpl @Inject() (postDAO: PostDAO) extends PostService {

  override def save(post: Post): Future[Post] = postDAO.save(post)

  override def delete(postId: String, user: User): Future[Boolean] = {
    postDAO.findById(postId).flatMap(postOpt => postOpt match {
      case Some(post) => {
        if (post.userId == user._id) {
          postDAO.delete(postId)
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
}
