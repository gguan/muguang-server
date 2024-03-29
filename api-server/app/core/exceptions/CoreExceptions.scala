package com.muguang.core.exceptions

import reactivemongo.core.commands.LastError

case class UnexpectedServiceException(message: String, nestedException: Throwable = null) extends ServiceException

case class DBServiceException(
  message: String,
  lastError: Option[LastError] = None,
  nestedException: Throwable = null) extends ServiceException

object DBServiceException {
  def apply(lastError: LastError): ServiceException = {
    DBServiceException(lastError.errMsg.getOrElse(lastError.message), Some(lastError))
  }
}
case class DuplicateResourceException(
  message: String = "error.duplicate.resource",
  nestedException: Throwable = null) extends ServiceException

case class OperationNotAllowedException(
  message: String = "error.operation.not.allowed",
  nestedException: Throwable = null) extends ServiceException

case class ResourceNotFoundException(
  id: String,
  message: String = "error.resource.not.found",
  nestedException: Throwable = null) extends ServiceException

case class InvalidResourceException(
  message: String = "error.invalid.resource",
  nestedException: Throwable = null) extends ServiceException
