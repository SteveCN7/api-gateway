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

package uk.gov.hmrc.apigateway.cache

import javax.inject.{Inject, Singleton}

import play.api.cache.CacheApi
import uk.gov.hmrc.apigateway.model.{VaryHeaderKey, VaryKey}

@Singleton
class VaryHeaderCacheManager @Inject()(cache: CacheApi) {

  def getKey(key: String, reqHeaders: Map[String, Set[String]]): String = {
    val key1 = VaryKey(key)
    val maybeStrings = cache.get[Set[String]](key1)
    maybeStrings match {
      case Some(varyHeaders) if varyHeaders.isEmpty => VaryHeaderKey(key).toString()
      case Some(varyHeaders) => VaryHeaderKey(key, getRelevantHeaders(varyHeaders, reqHeaders):_*)
      case _ => key
    }
  }

  private def getRelevantHeaders(varyHeaders: Set[String], reqHeaders: Map[String, Set[String]]) = {
    varyHeaders
      .map(x => reqHeaders.get(x)
      .map(h => (x, h.toSeq.sorted.mkString(","))))
      .flatten
      .toSeq
  }
}
