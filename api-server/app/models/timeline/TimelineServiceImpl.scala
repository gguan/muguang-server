package models.timeline

import javax.inject.Inject

import com.muguang.core.db.{ DBQueryBuilder, MongoHelper }
import models._
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.bson.BSONObjectID
import services.{ PostService, UserGraphService, TimelineService }

import scala.concurrent.Future

/**
 * Fanout on write to cache implementation
 */
class TimelineServiceImpl @Inject() (
  timelineDAO: TimelineDAO,
  usergraphService: UserGraphService,
  postService: PostService) extends TimelineService with MongoHelper {

  val CacheSizeLimit = 200

  override def feed(user: User, post: Post): Future[Unit] = {
    // only keep link and minimum information in cache
    val feedCache = post.toFeedCache

    // push feed to user timeline cache
    timelineDAO.collection.update(
      Json.obj("_id" -> user._id),
      DBQueryBuilder.pushToCappedArray("_c", Seq(feedCache), CacheSizeLimit),
      upsert = false,
      multi = true
    )

    // fanout to cache for each recipient, no upsert for recipient
    for {
      followers <- usergraphService.getFollowers(user._id)
      // push the feed to all recipient cache
      result <- timelineDAO.collection.update(
        DBQueryBuilder.in("_id", followers),
        DBQueryBuilder.pushToCappedArray("_c", Seq(feedCache), CacheSizeLimit),
        upsert = false,
        multi = true
      )
    } yield {}
  }

  override def getFeedFor(userId: BSONObjectID, limit: Int, anchor: Option[BSONObjectID]): Future[List[Post]] = {

    // if an anchor is used, we cannot predict a limit
    if (anchor.isDefined) {
      timelineDAO.findOne(DBQueryBuilder.id(userId)).map({
        case Some(cache) => cache.feeds.map(_._id)
        case None => List[BSONObjectID]()
      }).flatMap { cacheIds =>
        val source =
          if (cacheIds.size == 0) {
            // if cache is empty, we build cache for user
            for {
              users <- usergraphService.getFollowers(userId)
              posts <- postService.getPostFor(users :+ userId, CacheSizeLimit, None)
              timeline <- timelineDAO.insert(TimelineCache(userId, posts.map(_.toFeedCache).reverse, List()))
            } yield {
              timeline match {
                case Right(t) => posts.filter(_._id.time < anchor.get.time).take(limit)
                case Left(ex) => throw ex
              }
            }
          } else {
            // we get post objects from ids
            postService.getPosts(cacheIds.filter(_.time < anchor.get.time).take(limit))
          }
        // If the anchor filter out all feeds, defer to the post service
        source.flatMap { list =>
          if (list.size == 0) {
            for {
              users <- usergraphService.getFollowers(userId)
              posts <- postService.getPostFor(users :+ userId, limit, anchor)
            } yield {
              posts
            }
          } else {
            Future.successful(list)
          }
        }
      }
    } else {
      // if there is no anchor, we retrieve the cache or build cache if not exist
      timelineDAO.collection.find(
        DBQueryBuilder.id(userId),
        Json.obj("_c" -> Json.obj("$slice" -> -limit))
      ).cursor[TimelineCache].headOption.map({
          case Some(cache) => cache.feeds.map(_._id)
          case None => List[BSONObjectID]()
        }).flatMap { cacheIds =>
          if (cacheIds.size == 0) {
            // if cache is empty, we build cache for user
            for {
              users <- usergraphService.getFollowers(userId)
              posts <- postService.getPostFor(users :+ userId, CacheSizeLimit, anchor)
              timeline <- timelineDAO.insert(TimelineCache(userId, posts.map(_.toFeedCache).reverse, List()))
            } yield {
              timeline match {
                case Right(t) => posts.take(limit)
                case Left(ex) => throw ex
              }
            }
          } else {
            // we get post objects from ids
            postService.getPosts(cacheIds)
          }
        }

    }

  }

}
