package com.muguang.core.exceptions

trait ServiceException extends Exception {
  val message: String
  val nestedException: Throwable
}
