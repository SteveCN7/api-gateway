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
import play.api.Logger
import play.api.cache.CacheApi
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.apigateway.model.{PrimaryCacheKey, VaryCacheKey}
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, DurationInt}

class CacheManagerSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val serviceName = "api-definition"
    val cacheKey = "/endpoint/aa"
    val varyHeader = "X-Aaa"
    val varyCacheKey = VaryCacheKey(cacheKey)
    val cachedValue = "cachedValue"
    val updatedValue = "updatedValue"
    val metrics = mock[CacheMetrics]
    val emptyHeaders = Map.empty[String, Set[String]]

    def customFallBack(resp: EntityWithResponseHeaders[String]) = Future.successful(resp)

    def fallbackFunction = customFallBack((updatedValue, emptyHeaders))
    def fallbackFunctionWithCacheExpiry =
      customFallBack((updatedValue, Map(HeaderNames.CACHE_CONTROL -> Set("max-age=123"))))
    def fallbackFunctionWithNoCache =
      customFallBack((updatedValue, Map(HeaderNames.CACHE_CONTROL -> Set("no-cache"))))
    def fallbackFunctionWithNoCache2 =
      customFallBack((updatedValue, Map(HeaderNames.CACHE_CONTROL -> Set("no-cache","no-store","max-age=0"))))
    def fallbackFunctionWithCacheExpiryAndVary =
      customFallBack(
        (
          updatedValue,
          Map(
            HeaderNames.CACHE_CONTROL -> Set("max-age=123"),
            HeaderNames.VARY -> Set(varyHeader)
          )
        )
      )
    def cacheMan(cache: CacheApi = new FakeCacheApi()): CacheManager = {
      val vcm = new VaryHeaderCacheManager(cache)
      new CacheManager(cache, metrics, vcm)
    }
 }

  "Get cached item" should {
    "return cached value when present." in new Setup {
      val fakeCache = new FakeCacheApi(cacheKey -> cachedValue)
      val cm = cacheMan(fakeCache)

      await(cm.get[String](cacheKey, serviceName, fallbackFunctionWithCacheExpiry, Map.empty)) shouldBe cachedValue

      verify(metrics).cacheHit(serviceName)
      verifyNoMoreInteractions(metrics)
    }

    "return value from fallback function and update cache when cache header present and has a max-age value" in new Setup {
      val fakeCache = new FakeCacheApi()
      val cm = cacheMan(fakeCache)

      await(cm.get[String](cacheKey, serviceName, fallbackFunctionWithCacheExpiry, Map.empty)) shouldBe updatedValue

      fakeCache.get(cacheKey) shouldBe Some(updatedValue)
      fakeCache.getTtl(cacheKey) shouldBe Some(123 seconds)

      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(metrics)
    }

    "return value from fallback function but do not cache when cache header present and is no-cache" in new Setup {
      val fakeCache = new FakeCacheApi()
      val cm = cacheMan(fakeCache)

      await(cm.get[String](cacheKey, serviceName, fallbackFunctionWithNoCache, Map.empty)) shouldBe updatedValue

      fakeCache.isEmpty shouldBe true
      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(metrics)
    }

    "return value from fallback function but do not cache when no cache header is present" in new Setup {
      val fakeCache = new FakeCacheApi()
      val cm = cacheMan(fakeCache)

      await(cm.get[String](cacheKey, serviceName, fallbackFunction, Map.empty)) shouldBe updatedValue

      fakeCache.isEmpty shouldBe true
      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(metrics)
    }
  }

  "Caching of responses with a Vary header" should {
    "fetch fresh value and cache it when the Vary header is not in the cache" in new Setup {
      val newHeaderValue = "aaa"
      val newCacheKey = PrimaryCacheKey(cacheKey, Some(varyHeader), Map(varyHeader -> Set(newHeaderValue)))

      val fakeCache = new FakeCacheApi()
      val cm = cacheMan(fakeCache)

      val reqHeaders = Map(varyHeader -> Set(newHeaderValue))
      await(cm.get[String](cacheKey, serviceName, fallbackFunctionWithCacheExpiryAndVary, reqHeaders)) shouldBe updatedValue

      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(metrics)

      fakeCache.get(newCacheKey) shouldBe Some(updatedValue)
      fakeCache.get(VaryCacheKey(cacheKey)) shouldBe Some(varyHeader)
    }
  }

  "Caching of responses with a Vary header" should {
    "return cached value when present." in new Setup {
      val varyHeaderValue = "aaa"
      val key1 = PrimaryCacheKey(cacheKey, Some(varyHeader), Map(varyHeader -> Set(varyHeaderValue)))

      val fakeCache = new FakeCacheApi(
        VaryCacheKey(cacheKey) -> varyHeader,
        key1 -> cachedValue
      )
      val cm = cacheMan(fakeCache)

      val reqHeaders = Map(varyHeader -> Set(varyHeaderValue))
      await(cm.get[String](cacheKey, serviceName, fallbackFunction, reqHeaders)) shouldBe cachedValue

      verify(metrics).cacheHit(serviceName)
      verifyNoMoreInteractions(metrics)
    }

    "fetch fresh value and cache it when the header is different from a previous cached response" in new Setup {
      val previousHeaderValue = "aaa"
      val newHeaderValue = "bbb"
      val previousCacheKey = PrimaryCacheKey(cacheKey, Some(varyHeader), Map(varyHeader -> Set(previousHeaderValue)))
      val newCacheKey = PrimaryCacheKey(cacheKey, Some(varyHeader), Map(varyHeader -> Set(newHeaderValue)))

      val fakeCache = new FakeCacheApi(
        VaryCacheKey(cacheKey) -> varyHeader,
        previousCacheKey -> cachedValue
      )
      val cm = cacheMan(fakeCache)

      val reqHeaders = Map(varyHeader -> Set(newHeaderValue))
      await(cm.get[String](cacheKey, serviceName, fallbackFunctionWithCacheExpiryAndVary, reqHeaders)) shouldBe updatedValue

      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(metrics)

      fakeCache.get(newCacheKey) shouldBe Some(updatedValue)
    }

    "fetch fresh value and cache it when the returned vary header is different from a previous cached vary header" in new Setup {
      val fakeCache = new FakeCacheApi()
      val cm = cacheMan(fakeCache)

      val reqHeadersA1 = Map("X-Aaa" -> Set("aaa", "AAA"))
      val respA1 = customFallBack(("A Response", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Aaa"))))
      // Fetch result from fallback fn
      await(cm.get[String](cacheKey, serviceName, respA1, reqHeadersA1)) shouldBe "A Response"

      val reqHeadersA2 = Map("X-Aaa" -> Set("aaa", "AAA"))
      val respA2 = customFallBack(("SHOULD NEVER GET THIS", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Aaa"))))
      // Bring back cached result
      await(cm.get[String](cacheKey, serviceName, respA2, reqHeadersA2)) shouldBe "A Response"

      val reqHeadersB1 = Map("X-Aaa" -> Set("bbb"))
      val respB1 = customFallBack(("B Response", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Bbb"))))
      // different header value, so fetch from fallback fn
      await(cm.get[String](cacheKey, serviceName, respB1, reqHeadersB1)) shouldBe "B Response"

      val reqHeadersB2 = Map("X-Aaa" -> Set("bbb"))
      val respB2 = customFallBack(("NOR THIS", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Bbb"))))
      // Vary header changed in previous call, and neither that call or this had a X-Bbb header, so return cached value.
      await(cm.get[String](cacheKey, serviceName, respB2, reqHeadersB2)) shouldBe "B Response"

      val reqHeadersC = Map("X-Aaa" -> Set("aaa", "AAA"))
      val respC = customFallBack(("THIS NEITHER", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Bbb"))))
      // As above.
      await(cm.get[String](cacheKey, serviceName, respC, reqHeadersC)) shouldBe "B Response"

      val reqHeadersD = Map("X-Bbb" -> Set("otherval"))
      val respD = customFallBack(("A New Response", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Bbb"))))
      // This has the header, so is different from previous cached resp. Get new val from fallback fn
      await(cm.get[String](cacheKey, serviceName, respD, reqHeadersD)) shouldBe "A New Response"
    }

    "manage a series of requests with different values for the X-Aaa header, and fetching/pulling from cache where appropriate" in new Setup {
      val fakeCache = new FakeCacheApi()
      val cm = cacheMan(fakeCache)

      val respA = customFallBack(("A Response", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Aaa"))))
      val respA2 = customFallBack(("A2 Response", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Aaa"))))
      val respB = customFallBack(("B Response", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Aaa"))))
      val respB2 = customFallBack(("B2 Response", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Aaa"))))
      val respC = customFallBack(("C Response", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Aaa"))))
      val respC2 = customFallBack(("C2 Response", Map( HeaderNames.CACHE_CONTROL -> Set("max-age=123"), HeaderNames.VARY -> Set("X-Aaa"))))
      val shouldUseCacheResp = customFallBack(("NOTHING TO RETURN", Map.empty))

      val reqHeadersA = Map(varyHeader -> Set("123AAA"))
      val reqHeadersB = Map(varyHeader -> Set("123BBB"))
      val reqHeadersC = Map(varyHeader -> Set("123CCC"))

      fakeCache.isEmpty shouldBe true

      await(cm.get[String](cacheKey, serviceName, respA, reqHeadersA)) shouldBe "A Response"
      await(cm.get[String](cacheKey, serviceName, respB, reqHeadersB)) shouldBe "B Response"
      await(cm.get[String](cacheKey, serviceName, shouldUseCacheResp, reqHeadersB)) shouldBe "B Response"
      await(cm.get[String](cacheKey, serviceName, shouldUseCacheResp, reqHeadersA)) shouldBe "A Response"
      await(cm.get[String](cacheKey, serviceName, respC, reqHeadersC)) shouldBe "C Response"
      await(cm.get[String](cacheKey, serviceName, shouldUseCacheResp, reqHeadersC)) shouldBe "C Response"

      fakeCache.addTime(123 seconds)

      await(cm.get[String](cacheKey, serviceName, shouldUseCacheResp, reqHeadersA)) shouldBe "A Response"
      await(cm.get[String](cacheKey, serviceName, shouldUseCacheResp, reqHeadersB)) shouldBe "B Response"
      await(cm.get[String](cacheKey, serviceName, shouldUseCacheResp, reqHeadersC)) shouldBe "C Response"

      fakeCache.addTime(1 seconds)

      await(cm.get[String](cacheKey, serviceName, respA2, reqHeadersA)) shouldBe "A2 Response"
      await(cm.get[String](cacheKey, serviceName, respB2, reqHeadersB)) shouldBe "B2 Response"
      await(cm.get[String](cacheKey, serviceName, respC2, reqHeadersC)) shouldBe "C2 Response"
    }
  }
}

class FakeCacheApi(initialState: (String, Any)*) extends CacheApi {

  val cache = new mutable.HashMap[String, Any]()
  val ttlCache = new mutable.HashMap[String, Duration]()
  var currentTimeSecs: Duration = 0 seconds

  initialState.foreach { kv =>
    cache.update(kv._1, kv._2)
    ttlCache.update(kv._1, 30 seconds)
  }

  def isEmpty = cache.isEmpty

  def getTtl(key: String) = ttlCache.get(key)

  def addTime(secs: Duration): Unit = {
    currentTimeSecs = currentTimeSecs + secs
  }

  override def set(key: String, value: Any, expiration: Duration): Unit = {
    Logger.debug(s"Setting $key to $value")
    cache.put(key, value)
    ttlCache.put(key, expiration)
  }

  def expireCache(key: String) = {
    ttlCache.get(key).map { expiry =>
      if (expiry < currentTimeSecs) remove(key)
    }
  }

  override def get[T](key: String)(implicit evidence$2: ClassManifest[T]): Option[T] = {
    expireCache(key)
    cache.get(key).asInstanceOf[Option[T]]
  }

  override def getOrElse[A](key: String, expiration: Duration)(orElse: => A)(implicit evidence$1: ClassManifest[A]): A = {
    expireCache(key)
    cache.getOrElse(key, orElse).asInstanceOf[A]
  }

  override def remove(key: String): Unit = {
    cache.remove(key)
    ttlCache.remove(key)
  }
}
