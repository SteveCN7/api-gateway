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

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, DurationInt}

class CacheManagerSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val serviceName = "api-definition"
    val cacheKey = "/endpoint/aa"
    val varyHeader = "X-Aaa"
    val varyCacheKey = VaryKey(cacheKey)
    val cachedValue = "cachedValue"
    val updatedValue = "updatedValue"
    val metrics = mock[CacheMetrics]
    val emptyHeaders = Map.empty[String, Set[String]]

    def fallbackFunction = Future.successful((updatedValue, emptyHeaders))
    def fallbackFunctionWithCacheExpiry =
      Future.successful((updatedValue, Map(HeaderNames.CACHE_CONTROL -> Set("max-age=123"))))
    def fallbackFunctionWithNoCache =
      Future.successful((updatedValue, Map(HeaderNames.CACHE_CONTROL -> Set("no-cache"))))
    def fallbackFunctionWithNoCache2 =
      Future.successful((updatedValue, Map(HeaderNames.CACHE_CONTROL -> Set("no-cache","no-store","max-age=0"))))
    def fallbackFunctionWithCacheExpiryAndVary =
      Future.successful(
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
      val newCacheKey = VaryHeaderKey(cacheKey, varyHeader -> newHeaderValue)

      val fakeCache = new FakeCacheApi()
      val cm = cacheMan(fakeCache)

      val reqHeaders = Map(varyHeader -> Set(newHeaderValue))
      await(cm.get[String](cacheKey, serviceName, fallbackFunctionWithCacheExpiryAndVary, reqHeaders)) shouldBe updatedValue

      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(metrics)

      fakeCache.get(newCacheKey) shouldBe Some(updatedValue)
      fakeCache.get(VaryKey(cacheKey)) shouldBe Some(Set(varyHeader))
    }
  }

  "Caching of responses with a Vary header" should {
    "return cached value when present." in new Setup {
      val varyHeaderValue = "aaa"
      val key1 = VaryHeaderKey(cacheKey, varyHeader -> varyHeaderValue)

      val fakeCache = new FakeCacheApi(
        VaryKey(cacheKey) -> Set(varyHeader),
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
      val previousCacheKey = VaryHeaderKey(cacheKey, varyHeader -> previousHeaderValue)
      val newCacheKey = VaryHeaderKey(cacheKey, varyHeader -> newHeaderValue)

      val fakeCache = new FakeCacheApi(
        VaryKey(cacheKey) -> Set(varyHeader),
        previousCacheKey -> cachedValue
      )
      val cm = cacheMan(fakeCache)

      val reqHeaders = Map(varyHeader -> Set(newHeaderValue))
      await(cm.get[String](cacheKey, serviceName, fallbackFunctionWithCacheExpiryAndVary, reqHeaders)) shouldBe updatedValue

      verify(metrics).cacheMiss(serviceName)
      verifyNoMoreInteractions(metrics)

      fakeCache.get(newCacheKey) shouldBe Some(updatedValue)
    }

    "manage a series of requests with different values for the X-Aaa header, and fetching/pulling from cache where appropriate" in {

    }
  }
}

class FakeCacheApi(initialState: (String, Any)*) extends CacheApi {

  val cache = new mutable.HashMap[String, Any]()
  val ttlCache = new mutable.HashMap[String, Duration]()
  initialState.foreach { kv =>
    cache.update(kv._1, kv._2)
    ttlCache.update(kv._1, 30 seconds)
  }

  def isEmpty = cache.isEmpty
  def getTtl(key: String) = ttlCache.get(key)

  override def set(key: String, value: Any, expiration: Duration): Unit = {
    cache.put(key, value)
    ttlCache.put(key, expiration)
  }

  override def get[T](key: String)(implicit evidence$2: ClassManifest[T]): Option[T] =
    cache.get(key).asInstanceOf[Option[T]]

  override def getOrElse[A](key: String, expiration: Duration)(orElse: => A)(implicit evidence$1: ClassManifest[A]): A =
    cache.getOrElse(key, orElse).asInstanceOf[A]

  override def remove(key: String): Unit = {
    cache.remove(key)
    ttlCache.remove(key)
  }
}
