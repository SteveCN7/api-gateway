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

object VaryHeaderKey {
  def apply(key: String, varyHeaders: (String, String)*) =
    s"$key::${varyHeaders.sorted.map(kv => s"${kv._1}=${kv._2}").mkString (";")}"

  def fromVaryHeader(key: String, requiredHeaders: Set[String], actualHeaders: Map[String, Set[String]]) = {
    val out = apply(key, requiredHeaders
      .map(x => actualHeaders.get(x)
        .map(h => (x, h.toSeq.sorted.mkString(","))))
      .flatten
      .toSeq:_*
    )
    out
  }
}

object VaryKey {
  def apply(path: String) = {
    s"vary::$path"
  }
}
