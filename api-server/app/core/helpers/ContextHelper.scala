package com.muguang.core.helpers

import scala.concurrent.ExecutionContext

/**
 * Implicit contexts helper
 */
trait ContextHelper {

  implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

}
