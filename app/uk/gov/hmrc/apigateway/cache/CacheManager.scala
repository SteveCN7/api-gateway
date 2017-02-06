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

import play.api.Logger
import play.api.cache.CacheApi
import uk.gov.hmrc.apigateway.model.{CacheControl, VaryHeaderKey, VaryKey}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

@Singleton
class CacheManager @Inject()(cache: CacheApi, metrics: CacheMetrics, varyHeaderCache: VaryHeaderCacheManager) {

  def get[T: ClassTag](key: String,
                       serviceName: String,
                       fallbackFunction: => Future[EntityWithResponseHeaders[T]],
                       reqHeaders: Map[String, Set[String]]
                      ): Future[T] = {

    val keyWithRelevantVaryHeaders = varyHeaderCache.getKey(key, reqHeaders)

    cache.get[T](keyWithRelevantVaryHeaders) match {
      case Some(value) =>
        Logger.debug(s"Cache hit for key [$keyWithRelevantVaryHeaders]")
        metrics.cacheHit(serviceName)
        Future.successful(value)
      case _ =>
        Logger.debug(s"Cache miss for key [$keyWithRelevantVaryHeaders]")
        metrics.cacheMiss(serviceName)
        fetchFromService(key, reqHeaders, fallbackFunction)
    }
  }

  private def fetchFromService[T: ClassTag](
                                             key: String,
                                             reqHeaders: Map[String, Set[String]],
                                             fallbackFunction: => Future[EntityWithResponseHeaders[T]]
                                           ): Future[T] = {
    fallbackFunction map { case (result, respHeaders) => {
      CacheControl.fromHeaders(respHeaders) match {
        case CacheControl(false, Some(max), varyHeaders) if varyHeaders.isEmpty =>
          cache.set(key, result, max seconds)
        case CacheControl(false, Some(max), varyHeaders) =>
          cache.set(VaryKey(key), varyHeaders, max seconds)
          cache.set(VaryHeaderKey.fromVaryHeader(key, varyHeaders, reqHeaders), result, max seconds)
        case _ => // Anything else we do not cache.
      }
      result
    }}
  }
}
