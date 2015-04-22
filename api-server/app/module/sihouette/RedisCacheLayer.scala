package module.sihouette

import com.mohiva.play.silhouette.api.util.CacheLayer
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import org.joda.time.DateTime
import play.api.Play
import play.api.Play.current
import scalacache._
import redis._

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.reflect.ClassTag

import scala.concurrent.ExecutionContext.Implicits.global

class RedisCacheLayer extends CacheLayer {

  override def save[T](key: String, value: T, expiration: Int): Future[T] = {
    if (value.isInstanceOf[JWTAuthenticator]) {
      RedisCacheLayer.save[T](key, value,
        (value.asInstanceOf[JWTAuthenticator].expirationDate.getMillis - DateTime.now.getMillis).toInt / 1000)
    } else {
      RedisCacheLayer.save[T](key, value)
    }

  }

  override def remove(key: String): Future[Unit] = RedisCacheLayer.remove(key)

  override def find[T: ClassTag](key: String): Future[Option[T]] = RedisCacheLayer.find[T](key)
}

object RedisCacheLayer extends CacheLayer {

  val host = Play.application.configuration.getString("redis.host").get
  val port = Play.application.configuration.getInt("redis.port").get

  implicit val scalaCache = ScalaCache(RedisCache(host, port))

  override def save[T](key: String, value: T, expiration: Int): Future[T] = {
    put[T](key)(value, ttl = Some(expiration.seconds)).map(_ => value)
  }

  override def remove(key: String): Future[Unit] = remove(key)

  override def find[T: ClassTag](key: String): Future[Option[T]] = get[T](key)
}

