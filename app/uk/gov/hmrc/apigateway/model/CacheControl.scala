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

import scala.util.Try

case class CacheControl(noCache: Boolean, maxAgeSeconds: Option[Int], vary: Set[String])

object CacheControl {
  def fromHeaders(headers: Map[String, Set[String]], context: String = "") = {
    val cacheControlHeader = headers.get(HeaderNames.CACHE_CONTROL).map(splitValues)
    val varyHeader: Option[Seq[String]] = headers.get(HeaderNames.VARY).map(splitValues)

    CacheControl(
      noCache = cacheControlHeader.exists(_.contains("no-cache")),
      maxAgeSeconds = cacheControlHeader.flatMap(findMaxAge),
      vary = varyHeader.map(_.toSet).getOrElse(Set.empty)
    )
  }

  private def splitValues(values: Set[String]) = values.flatMap(_.split(",\\s*")).toSeq

  private def findMaxAge(vals: Seq[String]): Option[Int] = {
    val maxAgePattern = "max-age=(\\d+)".r
    vals.foldLeft[Option[Int]](None) {
      case (None, maxAgePattern(age)) => Some(Try(age.toInt).toOption.getOrElse(Int.MaxValue))
      case (a, _) => a
    }
  }
}
