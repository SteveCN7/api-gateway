/*
 * Copyright 2016 HM Revenue & Customs
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

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.cache.CacheApi
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class CacheManagerSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val serviceName = "api-definition"
    val cacheKey = "cacheKey"
    val cachedValue = "cachedValue"
    val updatedValue = "updatedValue"
    val cache = mock[CacheApi]
    val metrics = mock[CacheMetrics]
    val cacheManager = new CacheManager(cache, metrics)

    def fallbackF = Future.successful(updatedValue)
  }

  "Get cached item" should {

    "return cached value when present and caching is enabled" in new Setup {
      when(cache.get[String](cacheKey)).thenReturn(Some(cachedValue))

      await(cacheManager.get[String](cacheKey, serviceName, fallbackF, true, 30)) shouldBe cachedValue

      verify(cache).get[String](cacheKey)
      verify(metrics).cacheHit(serviceName)
      verifyNoMoreInteractions(cache, metrics)
    }

    "return value from fallback function and update cache when caching is enabled" in new Setup {
      when(cache.get[String](cacheKey)).thenReturn(None)

      await(cacheManager.get[String](cacheKey, serviceName, fallbackF, true, 30)) shouldBe updatedValue

      verify(cache).get[String](cacheKey)
      verify(cache).set(cacheKey, updatedValue, 30 seconds)
      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(cache, metrics)
    }

    "return value from fallback function and ignore caching when disabled" in new Setup {
      await(cacheManager.get[String](cacheKey, serviceName, fallbackF, false)) shouldBe updatedValue
      verifyZeroInteractions(cache, metrics)
    }
  }
}