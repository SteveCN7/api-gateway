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
import play.api.http.HeaderNames.CACHE_CONTROL
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

    val newKey = varyHeaderCache.getKey(key, reqHeaders)
    val varyKey = VaryKey(key)

    cache.get[T](newKey) match {
      case Some(value) =>
        processCacheHit(newKey, serviceName, value)
      case _ =>
        processCacheMiss(newKey, varyKey, serviceName, fallbackFunction)
    }
  }

  private def processCacheHit[T: ClassTag](key: String, serviceName: String, value: T): Future[T] = {
    Logger.debug(s"Cache hit for key [$key]")
    metrics.cacheHit(serviceName)
    Future.successful(value)
  }

  private def processCacheMiss[T: ClassTag](key: String, varyKey: String, serviceName: String, fallbackFunction: => Future[(T, Map[String, Set[String]], Map[String, Set[String]])]): Future[T] = {
    Logger.debug(s"Cache miss for key [$key]")
    metrics.cacheMiss(serviceName)
    fallbackFunction map { case (result, reqHeaders, respHeaders) =>
      CacheControl.fromHeaders(respHeaders) match {
        case CacheControl(false, Some(max), varyHeaders) if varyHeaders.isEmpty => cache.set(key, result, max seconds)
        case CacheControl(false, Some(max), varyHeaders) if varyHeaders.nonEmpty => {
          cache.set(varyKey, varyHeaders, max seconds)
          cache.set(varyHeaderCache.getKey(key, respHeaders), result, max seconds)
        }
        case _ => println("Nothing to do yet...")
      }
      result
    }
  }
}
