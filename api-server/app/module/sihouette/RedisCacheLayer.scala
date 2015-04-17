package module.sihouette

import play.api.Play
import play.api.Play.current
import redis.clients.jedis.Jedis

import scala.concurrent.Future

class RedisCacheLayer {

  val host = Play.application.configuration.getString("redis.host").get
  val port = Play.application.configuration.getInt("redis.port").get

  val jedis = new Jedis(host, port)

  def save(key: String, value: String, expiration: Int): Future[String] = {
    jedis.setex(key, expiration, value.toString)
    Future.successful(value)
  }

  def remove(key: String): Future[Unit] = {
    jedis.del(key)
    Future.successful((): Unit)
  }

  def find(key: String): Future[Option[String]] = {
    Future.successful(Option(jedis.get(key)))
  }

}
