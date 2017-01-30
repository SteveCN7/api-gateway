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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

@Singleton
class CacheManager @Inject()(cache: CacheApi, metrics: CacheMetrics) {

  def get[T: ClassTag](key: String,
                       serviceName: String,
                       fallbackFunction: => Future[EntityWithResponseHeaders[T]]): Future[T] = {

    cache.get[T](key) match {
      case Some(value) =>
        Logger.debug(s"Cache hit for key [$key]")
        metrics.cacheHit(serviceName)
        Future.successful(value)
      case _ =>
        //        Logger.debug(s"Cache miss for key [$key]. Caching flag is set to: [$caching] with expiration: [$expiration]")
        Logger.debug(s"Cache miss for key [$key]")
        metrics.cacheMiss(serviceName)
        fallbackFunction map { case (t, headers) =>
          headers.get(CACHE_CONTROL) map {
            case cacheControl if !cacheControl.toLowerCase.equals("no-cache") =>
              Logger.debug(s"Caching1 response for key [$key] with expiration [${cacheControl.toInt.seconds}] seconds")
              cache.set(key, t, cacheControl.toInt.seconds)
          }
          t
        }
    }
  }
}
