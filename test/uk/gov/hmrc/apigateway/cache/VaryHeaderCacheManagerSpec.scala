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
import uk.gov.hmrc.apigateway.model.PrimaryCacheKey
import uk.gov.hmrc.play.test.UnitSpec

class VaryHeaderCacheManagerSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val serviceName = "api-definition"
    val cacheKey = "/endpoint/aa"
    val varyCacheKey = s"vary::$cacheKey"
    val updatedValue = "updatedValue"
    val cache = mock[CacheApi]
    val varyCacheManager = new VaryHeaderCacheManager(cache)
 }

  "Get cached item" should {
    "return original key if there are no vary headers stored" in new Setup {
      when(cache.get[String](varyCacheKey)).thenReturn(None)

      await(varyCacheManager.getKey(cacheKey, Map.empty)) shouldBe cacheKey

      verify(cache).get[String](varyCacheKey)
      verifyNoMoreInteractions(cache)
    }

    "return the key with an empty header value if the request does not have any request headers" in new Setup {
      val cachedValue = "X-Aaa"
      when(cache.get[String](varyCacheKey)).thenReturn(Some(cachedValue))

      await(varyCacheManager.getKey(cacheKey, Map.empty)) shouldBe s"$cacheKey::X-Aaa="

      verify(cache).get[String](varyCacheKey)
      verifyNoMoreInteractions(cache)
    }

    "return the key with an empty header value if the request does not have any matching headers" in new Setup {
      val cachedValue = "X-Aaa"
      when(cache.get[String](varyCacheKey)).thenReturn(Some(cachedValue))

      await(varyCacheManager.getKey(cacheKey, Map("Bob" -> Set("Blah")))) shouldBe s"$cacheKey::X-Aaa="

      verify(cache).get[String](varyCacheKey)
      verifyNoMoreInteractions(cache)
    }

    "return the representation of the key if the request has the matching header" in new Setup {
      val cachedValue = "X-Aaa"
      val reqHeaders = Map("X-Aaa" -> Set("aaa"))
      when(cache.get[String](varyCacheKey)).thenReturn(Some(cachedValue))

      await(varyCacheManager.getKey(cacheKey, reqHeaders)) shouldBe PrimaryCacheKey(cacheKey, Some("X-Aaa"), reqHeaders)

      verify(cache).get[String](varyCacheKey)
      verifyNoMoreInteractions(cache)
    }
  }
}
