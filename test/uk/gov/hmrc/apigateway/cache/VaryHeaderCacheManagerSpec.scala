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
import uk.gov.hmrc.apigateway.model.VaryHeaderKey
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class VaryHeaderCacheManagerSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val serviceName = "api-definition"
    val cacheKey = "cacheKey"
    val varyCacheKey = s"vary::$cacheKey"
    val updatedValue = "updatedValue"
    val cache = mock[CacheApi]
    val cacheManager = new VaryHeaderCacheManager(cache)
 }

  "Get cached item" should {
    "return original key if there are no vary headers stored" in new Setup {
      when(cache.get[Set[String]](varyCacheKey)).thenReturn(None)

      await(cacheManager.getKey(cacheKey, Map.empty)) shouldBe cacheKey

      verify(cache).get[Set[String]](varyCacheKey)
      verifyNoMoreInteractions(cache)
    }

    "return the key suffixed :: if the request does not have any request headers" in new Setup {
      val cachedValue = Set("X-Aaa")
      when(cache.get[Set[String]](varyCacheKey)).thenReturn(Some(cachedValue))

      await(cacheManager.getKey(cacheKey, Map.empty)) shouldBe s"$cacheKey::"

      verify(cache).get[Set[String]](varyCacheKey)
      verifyNoMoreInteractions(cache)
    }

    "return the key suffixed :: if the request does not have any matching headers" in new Setup {
      val cachedValue = Set("X-Aaa")
      when(cache.get[Set[String]](varyCacheKey)).thenReturn(Some(cachedValue))

      await(cacheManager.getKey(cacheKey, Map("Bob" -> Set("Blah")))) shouldBe s"$cacheKey::"

      verify(cache).get[Set[String]](varyCacheKey)
      verifyNoMoreInteractions(cache)
    }

    "return the representation of the key if the request the matching header" in new Setup {
      val cachedValue = Set("X-Aaa")
      val reqHeaders = Map("X-Aaa" -> Set("aaa"))
      when(cache.get[Set[String]](varyCacheKey)).thenReturn(Some(cachedValue))

      await(cacheManager.getKey(cacheKey, reqHeaders)) shouldBe VaryHeaderKey(cacheKey, "X-Aaa" -> "aaa").toString

      verify(cache).get[Set[String]](varyCacheKey)
      verifyNoMoreInteractions(cache)
    }

    "return the representation of the key if the request has all matching headers" in new Setup {
      val cachedValue = Set("X-Aaa", "X-Bbb")
      val reqHeaders = Map("X-Aaa" -> Set("aaa"), "X-Bbb" -> Set("bbb"))
      when(cache.get[Set[String]](varyCacheKey)).thenReturn(Some(cachedValue))

      await(cacheManager.getKey(cacheKey, reqHeaders)) shouldBe VaryHeaderKey(cacheKey, "X-Aaa" -> "aaa", "X-Bbb" -> "bbb").toString

      verify(cache).get[Set[String]](varyCacheKey)
      verifyNoMoreInteractions(cache)
    }
  }
}
