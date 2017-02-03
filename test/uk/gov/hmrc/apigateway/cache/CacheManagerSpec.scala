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

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.cache.CacheApi
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.apigateway.model.{VaryHeaderKey, VaryKey}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class CacheManagerSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val serviceName = "api-definition"
    val cacheKey = "/endpoint/aa"
    val varyCacheKey = VaryKey(cacheKey)
    val cachedValue = "cachedValue"
    val updatedValue = "updatedValue"
    val cache = mock[CacheApi]
    val metrics = mock[CacheMetrics]
    val varyHeaderCacheManager = new VaryHeaderCacheManager(cache)
    val cacheManager = new CacheManager(cache, metrics, varyHeaderCacheManager)

    def fallbackFunction = Future.successful((updatedValue, Map.empty[String, Set[String]], Map.empty[String, Set[String]]))
    def fallbackFunctionWithCacheExpiry =
      Future.successful((updatedValue, Map.empty[String, Set[String]], Map(HeaderNames.CACHE_CONTROL -> Set("max-age=123"))))
    def fallbackFunctionWithNoCache =
      Future.successful((updatedValue, Map.empty[String, Set[String]], Map(HeaderNames.CACHE_CONTROL -> Set("no-cache"))))
    def fallbackFunctionWithNoCache2 =
      Future.successful((updatedValue, Map.empty[String, Set[String]], Map(HeaderNames.CACHE_CONTROL -> Set("no-cache","no-store","max-age=0"))))
    def fallbackFunctionWithCacheExpiryAndVary =
      Future.successful((updatedValue, Map.empty[String, Set[String]], Map(
          HeaderNames.CACHE_CONTROL -> Set("max-age=123"),
          HeaderNames.VARY -> Set("X-Blah")
        )
      ))
 }

  "Get cached item" should {
    "return cached value when present." in new Setup {
      when(cache.get[Set[String]](VaryKey(cacheKey))).thenReturn(None)
      when(cache.get[String](cacheKey)).thenReturn(Some(cachedValue))

      await(cacheManager.get[String](cacheKey, serviceName, fallbackFunctionWithCacheExpiry, Map.empty)) shouldBe cachedValue

      verify(cache).get[Set[String]](VaryKey(cacheKey))
      verify(cache).get[String](cacheKey)
      verify(metrics).cacheHit(serviceName)
      verifyNoMoreInteractions(cache, metrics)
    }

    "return value from fallback function and update cache when cache header present and has a max-age value" in new Setup {
      when(cache.get[Set[String]](VaryKey(cacheKey))).thenReturn(None)
      when(cache.get[String](cacheKey)).thenReturn(None)

      await(cacheManager.get[String](cacheKey, serviceName, fallbackFunctionWithCacheExpiry, Map.empty)) shouldBe updatedValue

      verify(cache).get[Set[String]](VaryKey(cacheKey))
      verify(cache).get[String](cacheKey)
      verify(cache).set(cacheKey, updatedValue, 123 seconds)
      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(cache, metrics)
    }

    "return value from fallback function but do not cache when cache header present and is no-cache" in new Setup {
      when(cache.get[Set[String]](VaryKey(cacheKey))).thenReturn(None)
      when(cache.get[String](cacheKey)).thenReturn(None)

      await(cacheManager.get[String](cacheKey, serviceName, fallbackFunctionWithNoCache, Map.empty)) shouldBe updatedValue

      verify(cache).get[Set[String]](VaryKey(cacheKey))
      verify(cache).get[String](cacheKey)
      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(cache, metrics)
    }

    "return value from fallback function but do not cache when no cache header is present" in new Setup {
      when(cache.get[Set[String]](VaryKey(cacheKey))).thenReturn(None)
      when(cache.get[String](cacheKey)).thenReturn(None)

      await(cacheManager.get[String](cacheKey, serviceName, fallbackFunction, Map.empty)) shouldBe updatedValue

      verify(cache).get[Set[String]](VaryKey(cacheKey))
      verify(cache).get[String](cacheKey)
      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(cache, metrics)
    }

    "return value from fallback function but do not cache when cache header is present but a Vary header is present." in new Setup {
      when(cache.get[Set[String]](VaryKey(cacheKey))).thenReturn(None)
      when(cache.get[String](cacheKey)).thenReturn(None)

      await(cacheManager.get[String](cacheKey, serviceName, fallbackFunctionWithCacheExpiryAndVary, Map.empty)) shouldBe updatedValue

      verify(cache).get[Set[String]](VaryKey(cacheKey))
      verify(cache).get[String](cacheKey)
      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(cache, metrics)
    }
  }

  "Get cached item with Vary header" should {
    "return cached value when present." in new Setup {
      private val varyHeader = "X-Aaa"
      private val varyHeaderValue = "aaa"
      private val key1 = VaryHeaderKey(cacheKey, varyHeader -> varyHeaderValue)

      when(cache.get[String](key1)).thenReturn(Some(cachedValue))
      when(cache.get[Set[String]](VaryKey(cacheKey))).thenReturn(Some(Set(varyHeader)))

      private val reqHeaders = Map(varyHeader -> Set(varyHeaderValue))
      await(cacheManager.get[String](cacheKey, serviceName, fallbackFunction, reqHeaders)) shouldBe cachedValue

      verify(cache).get[Set[String]](VaryKey(cacheKey))
      verify(cache).get[String](key1)
      verify(metrics).cacheHit(serviceName)
      verifyNoMoreInteractions(cache, metrics)
    }
  }
}
