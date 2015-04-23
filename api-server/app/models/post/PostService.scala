package models.post

import models._
import play.api.libs.json.JsObject
import play.extras.geojson.LatLng

import scala.concurrent.Future

trait PostService {

  def save(post: Post): Future[Post]

  def deleteByUser(postId: String, user: User): Future[Boolean]

  def createPost(postCommand: CreatePostCommand, user: User): Post

  def countPostByUserId(userId: String): Future[Int]

  def getRecentPostsByUserId(userId: String, skip: Int, limit: Int): Future[List[PostSummary]]

  def commentPost(postId: String, comment: Comment): Future[Comment]

  def deleteCommentByUser(postId: String, commentId: String, user: User): Future[Boolean]

  def likePost(postId: String, emotion: PostEmotion): Future[PostEmotion]

  def unlikePost(postId: String, user: User): Future[Boolean]

  def getOneRandomPost: Future[Option[Post]]

  def searchNearbyPosts(latLng: LatLng, maxDistance: Option[Int] = None, minDistance: Option[Int] = None, query: Option[JsObject] = None): Future[List[PostDistanceResult]]

}
