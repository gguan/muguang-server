package models.post

import com.muguang.core.dao.DocumentDao
import models.Post

import scala.concurrent.Future

trait PostDAO extends DocumentDao[Post] {

  def save(post: Post): Future[Post]

  def delete(postId: String): Future[Boolean]

}
