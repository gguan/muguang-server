package utils.sihouette

import com.mohiva.play.silhouette.api.util.CacheLayer
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.daos.AuthenticatorDAO
import org.joda.time.DateTime

import scala.concurrent.Future
import scala.pickling.Defaults._
import scala.pickling.json._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Implementation of the authenticator DAO which uses the cache layer to persist the authenticator.
 *
 * @param cacheLayer The cache layer implementation.
 */
class RedisCacheBearerTokenAuthenticatorDAO(cacheLayer: CacheLayer) extends AuthenticatorDAO[BearerTokenAuthenticator] {

  /**
   * Saves the authenticator.
   *
   * @param authenticator The authenticator to save.
   * @return The saved auth info.
   */
  def save(authenticator: BearerTokenAuthenticator): Future[BearerTokenAuthenticator] = {
    cacheLayer.save[String](
      authenticator.id,
      authenticator.pickle.value,
      (authenticator.expirationDate.getMillis - DateTime.now.getMillis).toInt / 1000
    ).map(x => authenticator)
  }

  /**
   * Finds the authenticator for the given ID.
   *
   * @param id The authenticator ID.
   * @return The found authenticator or None if no authenticator could be found for the given ID.
   */
  def find(id: String): Future[Option[BearerTokenAuthenticator]] = {
    cacheLayer.find[String](id).map {
      authenticatorOpt => authenticatorOpt.map(str => JSONPickle(str).unpickle[BearerTokenAuthenticator])
    }
  }

  /**
   * Removes the authenticator for the given ID.
   *
   * @param id The authenticator ID.
   * @return An empty future.
   */
  def remove(id: String): Future[Unit] = cacheLayer.remove(id)

}
