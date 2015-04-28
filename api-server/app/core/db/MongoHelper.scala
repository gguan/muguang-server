package com.muguang.core.db

import com.muguang.core.exceptions._
import play.api.Logger
import play.api.Play.current
import play.modules.reactivemongo.ReactiveMongoPlugin

import reactivemongo.bson.{ BSONObjectID, BSONValue }

import com.muguang.core.helpers.ContextHelper
import reactivemongo.core.commands.LastError
import reactivemongo.core.errors.DatabaseException

import scala.concurrent.Future

trait MongoHelper extends ContextHelper {

  lazy val db = ReactiveMongoPlugin.db

  def Recover[S](operation: Future[LastError])(success: => S): Future[Either[ServiceException, S]] = {
    operation.map {
      lastError =>
        lastError.inError match {
          case true =>
            Logger.error(s"DB operation did not perform successfully: [lastError=$lastError]")
            Left(DBServiceException(lastError))

          case false => Right(success)
        }
    } recover {
      case exception =>
        Logger.error(s"DB operation failed: [message=${exception.getMessage}]")

        //TODO: better failure handling here
        val handling: Option[Either[ServiceException, S]] = exception match {
          case e: DatabaseException =>
            e.code.map(code => {
              Logger.error(s"DatabaseException: [code=$code, isNotAPrimaryError=${e.isNotAPrimaryError}]")
              code match {
                case 10148 => Left(OperationNotAllowedException("", nestedException = e))
                case 11000 => Left(DuplicateResourceException(nestedException = e))
              }
            })

        }
        handling.getOrElse(Left(UnexpectedServiceException(exception.getMessage, nestedException = exception)))
    }
  }

  def UnsafeRecover[S](operation: Future[LastError])(success: => S): Future[S] = {
    operation.map {
      lastError =>
        lastError.inError match {
          case true =>
            Logger.error(s"DB operation did not perform successfully: [lastError=$lastError]")
            throw DBServiceException(lastError)

          case false => success
        }
    } recover {
      case exception =>
        Logger.error(s"DB operation failed: [message=${exception.getMessage}]")

        //TODO: better failure handling here
        exception match {
          case e: DatabaseException =>
            e.code.map(code => {
              Logger.error(s"DatabaseException: [code=$code, isNotAPrimaryError=${e.isNotAPrimaryError}]")
              code match {
                case 10148 => throw OperationNotAllowedException("", nestedException = e)
                case 11000 => throw DuplicateResourceException(nestedException = e)
              }
            })
          case e => UnexpectedServiceException(exception.getMessage, nestedException = exception)
        }
        success
    }
  }

  def HandleDBFailure[T](f: Future[Either[ServiceException, T]]): Future[T] = {
    f.map {
      case Right(doc) => doc
      case Left(ex) => throw ex
    }
  }

}

object MongoHelper extends MongoHelper {
  def identify(bson: BSONValue) = bson.asInstanceOf[BSONObjectID].stringify
}
