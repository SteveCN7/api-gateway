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

import com.codahale.metrics.{Counter, MetricRegistry}
import com.kenshoo.play.metrics.Metrics
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class CacheMetricsSpec extends UnitSpec with MockitoSugar {
  trait Setup {
    val serviceName = "api-definition"
    val hitCounterName = s"cache-hit:$serviceName"
    val missCounterName = s"cache-miss:$serviceName"

    val metrics = mock[Metrics]
    val registry = mock[MetricRegistry]
    val hitCounter = mock[Counter]
    val missCounter = mock[Counter]

    when(metrics.defaultRegistry).thenReturn(registry)
    when(registry.counter(hitCounterName)).thenReturn(hitCounter)
    when(registry.counter(missCounterName)).thenReturn(missCounter)

    val cacheMetrics = new CacheMetrics(metrics)
  }

  "Cache metrics" should {
    "record a cache hit for the correct counter" in new Setup {
      cacheMetrics.cacheHit(serviceName)
      verify(registry).counter(hitCounterName)
      verify(hitCounter).inc()
      verifyNoMoreInteractions(registry, hitCounter, missCounter)
    }

    "record a cache miss for the correct counter" in new Setup {
      cacheMetrics.cacheMiss(serviceName)
      verify(registry).counter(missCounterName)
      verify(missCounter).inc()
      verifyNoMoreInteractions(registry, hitCounter, missCounter)
    }
  }
}
