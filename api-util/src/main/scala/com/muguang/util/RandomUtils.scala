package com.muguang.util

import java.security.SecureRandom

object RandomUtils {

  val random = new SecureRandom()

  def generateToken(): String = {
    new java.math.BigInteger(130, random).toString(64)
  }

}
