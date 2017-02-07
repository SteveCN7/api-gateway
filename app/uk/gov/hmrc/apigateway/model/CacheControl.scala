/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apigateway.model

import play.mvc.Http.HeaderNames

case class CacheControlException(message: String, cause: Throwable = null) extends Exception(message, cause)

case class CacheControl(noCache: Boolean, maxAgeSeconds: Option[Int], vary: Option[String])

object CacheControl {
  def fromHeaders(originalHeaders: Map[String, Set[String]]) = {
    val headers = originalHeaders.mapValues(_.flatMap(_.split(",\\s*")).toSeq)
    val defaults = (false, None, None)
    val params: (Boolean, Option[Int], Option[String]) = headers.foldLeft[(Boolean, Option[Int], Option[String])] (defaults){
        case (a, (HeaderNames.CACHE_CONTROL, vals)) =>
          (vals.contains("no-cache"), findMaxAge(vals), a._3)
        case (a, (HeaderNames.VARY, Seq(header))) =>
          (a._1, a._2, Some(header))
        case (a, (HeaderNames.VARY, seq)) if seq.isEmpty =>
          (a._1, a._2, None)
        case (a, (HeaderNames.VARY, headers)) =>
          throw CacheControlException(s"Multiple Vary headers are not supported for caching. (Headers: ${headers.mkString(", ")})")
        case (a, _) => a
      }
    CacheControl(params._1, params._2, params._3)
  }

  private def findMaxAge(vals: Seq[String]): Option[Int] = {
    val maxAgePattern = "max-age=(\\d+)".r
    vals.foldLeft[Option[Int]](None) {
      case (None, maxAgePattern(age)) => Some(age.toInt)
      case (a, _) => a
    }
  }
}
