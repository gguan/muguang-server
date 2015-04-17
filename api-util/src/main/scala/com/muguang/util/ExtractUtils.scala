package com.muguang.util

import com.twitter.Extractor
import collection.JavaConversions._

object ExtractUtils {

  val extractor = new Extractor()

  def extractHashtags(content: String): Set[String] = {
    extractor.extractHashtags(content).toSet
  }

}
