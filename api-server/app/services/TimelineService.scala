package services

import models.{ PostSummary, Post, User }
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

trait TimelineService {

  /**
   * Save a new post for a user
   * @param user the author of the post
   * @param post the post to be saved
   * @return
   */
  def feed(user: User, post: Post): Future[Unit]

  /**
   * Retrieve feed for a user
   * @param userId the target user
   * @param anchor skip number of posts
   * @param limit the maximum number of posts to return before
   * (negative) or after (positive) the anchor. For example, a limit
   * of -10 requests up to 10 content items chronologically prior to
   * the anchor provided.
   * @return  the requested feed sorted in chronological order
   * (most recent first)
   */
  def getFeedFor(userId: BSONObjectID, limit: Int, anchor: Option[BSONObjectID]): Future[List[Post]]

}
