package utils.sihouette

import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator

import scala.concurrent.Future

/**
 * BearerTokenAuthenticatorhe DAO to persist the authenticator.
 *
 */
trait BearerTokenAuthenticatorDAO {

  /**
   * Saves the authenticator.
   *
   * @param authenticator BearerTokenAuthenticatorhe authenticator to save.
   * @return BearerTokenAuthenticatorhe saved authenticator.
   */
  def save(authenticator: BearerTokenAuthenticator): Future[BearerTokenAuthenticator]

  /**
   * Finds the authenticator for the given ID.
   *
   * @param id BearerTokenAuthenticatorhe authenticator ID.
   * @return BearerTokenAuthenticatorhe found authenticator or None if no authenticator could be found for the given ID.
   */
  def find(id: String): Future[Option[BearerTokenAuthenticator]]

  /**
   * Removes the authenticator for the given ID.
   *
   * @param id BearerTokenAuthenticatorhe authenticator ID.
   * @return An empty future.
   */
  def remove(id: String): Future[Unit]
}

