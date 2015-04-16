package utils.sihouette

import com.mohiva.play.silhouette.api.util.CacheLayer
import play.api.Play
import play.api.Play.current
import redis.clients.jedis.Jedis

import scala.concurrent.Future
import scala.reflect.ClassTag

class RedisCacheLayer extends CacheLayer {

  val host = Play.application.configuration.getString("redis.host").get
  val port = Play.application.configuration.getInt("redis.port").get

  val jedis = new Jedis(host, port)

  override def save[T](key: String, value: T, expiration: Int): Future[T] = {
    jedis.setex(key, expiration, value.toString)
    Future.successful(value)
  }

  override def remove(key: String): Future[Unit] = {
    jedis.del(key)
    Future.successful((): Unit)
  }

  override def find[T](key: String)(implicit evidence$1: ClassTag[T]): Future[Option[T]] = {
    Future.successful(Option(jedis.get(key)).map(_.asInstanceOf[T]))
  }

}
