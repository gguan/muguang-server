package services

import models._
import play.api.libs.json.JsObject
import play.extras.geojson.LatLng
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

/**
 * Handles actions to posts.
 */
trait PostService {

  /**
   * Validate if a post command is valid
   * @param postCommand
   * @param user the user that authored the post
   * @return the Post object
   */
  def validatePostCommand(postCommand: CreatePostCommand, user: User): Post

  /**
   * Return full post item by its ID
   * @param postId the ID of the Post to return
   * @return the Post with the given ID or None, if
   *         no post exists with that ID
   */
  def getPostById(postId: BSONObjectID): Future[Option[Post]]

  /**
   * Delete a post by its author
   * @param postId the ID of the post to delete
   * @param user author
   * @return
   */
  def deletePost(postId: BSONObjectID, user: User): Future[Unit]

  /**
   * Publish post by a user
   * @param user author of the post
   * @param post post object
   * @return
   */
  def publishPost(user: User, post: Post): Future[Post]

  /**
   * Count total number of posts by user ID
   * @param userId the ID of user
   * @return count
   */
  def countPostFor(userId: BSONObjectID): Future[Int]

  /**
   * Find and return content previously published for a specified user
   * @param userId the ID of user for which to retrieve content
   * @param anchor a point in content history about which to anchor
   *               the results. The anchor may be the id of any content (even content
   *               authored by an arbitrary user). A null anchor implies the latest
   *               content for the specified user is requested.
   * @param limit the maximum number of Content items to return before
   *              (negative) or after (positive) the anchor. For example, a limit
   *              of -10 requests up to 10 content items chronologically prior to
   *              the anchor provided.
   * @return the requested content sorted in chronological order
   *         (most recent first)
   */
  def getPostFor(userId: BSONObjectID, limit: Int, anchor: Option[BSONObjectID]): Future[List[Post]]

  /**
   * Find and return content previously published for a specified user list
   * @param users the users for which to retrieve content
   * @param anchor a point in content history about which to anchor
   *               the results. The anchor may be the id of any content (even content
   *               authored by an arbitrary user). A null anchor implies the latest
   *               content for the specified users is requested.
   * @param limit the maximum number of Content items to return before
   *              (negative) or after (positive) the anchor. For example, a limit
   *              of -10 requests up to 10 content items chronologically prior to
   *              the anchor provided.
   * @return the requested content sorted in chronological order
   *         (most recent first).
   */
  def getPostFor(users: Seq[BSONObjectID], limit: Int, anchor: Option[BSONObjectID]): Future[List[Post]]

  def commentPost(postId: String, comment: Comment): Future[Comment]

  def deleteCommentByUser(postId: String, commentId: String, user: User): Future[Boolean]

  def likePost(postId: String, emotion: PostEmotion): Future[PostEmotion]

  def unlikePost(postId: String, user: User): Future[Boolean]

  def getOneRandomPost: Future[Option[Post]]

  def searchNearbyPosts(latLng: LatLng, maxDistance: Option[Int] = None, minDistance: Option[Int] = None, query: Option[JsObject] = None): Future[List[PostDistanceResult]]

}
