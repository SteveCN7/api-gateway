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

import play.api.Logger
import play.mvc.Http.HeaderNames

import scala.util.Try

case class CacheControlException(message: String, cause: Throwable = null) extends Exception(message, cause)

case class CacheControl(noCache: Boolean, maxAgeSeconds: Option[Int], vary: Option[String])

object CacheControl {
  def fromHeaders(originalHeaders: Map[String, Set[String]], context: String = "") = {
      val headers = originalHeaders.mapValues(_.flatMap(_.split(",\\s*")).toSeq)
      headers.foldLeft[CacheControl](CacheControl(true, None, None)) {
        case (a, (HeaderNames.CACHE_CONTROL, vals)) =>
          a.copy(noCache = a.noCache && vals.contains("no-cache"), maxAgeSeconds = findMaxAge(vals))
        case (a, (HeaderNames.VARY, Seq(header))) =>
          a.copy(vary = Some(header))
        case (a, (HeaderNames.VARY, Seq())) =>
          a.copy(vary = None)
        case (a, (HeaderNames.VARY, headers)) =>
          Logger.warn(s"($context) Multiple Vary headers are not supported for caching. (Headers: ${headers.mkString(", ")})")
          CacheControl(true, None, None)
        case (a, _) => a
      }
  }

  private def findMaxAge(vals: Seq[String]): Option[Int] = {
    val maxAgePattern = "max-age=(\\d+)".r
    vals.foldLeft[Option[Int]](None) {
      case (None, maxAgePattern(age)) => Some(age.toInt)
      case (a, _) => a
    }
  }
}
