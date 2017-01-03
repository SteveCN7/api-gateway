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

import com.kenshoo.play.metrics.Metrics

@Singleton
class CacheMetrics @Inject() (metrics: Metrics) {
  def cacheMiss(serviceName: String): Unit = metrics.defaultRegistry.counter(s"cache-miss:$serviceName").inc()
  def cacheHit(serviceName: String): Unit = metrics.defaultRegistry.counter(s"cache-hit:$serviceName").inc()
}
