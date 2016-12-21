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

package uk.gov.hmrc.apigateway.connector

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.cache.CacheApi
import play.api.libs.json.Json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import uk.gov.hmrc.apigateway.cache.{CacheMetrics, CacheManager}

import scala.concurrent.Future._

// TODO use wiremock and get rid of this
trait WsClientMocking extends MockitoSugar {

  private val cacheApi = mock[CacheApi]
  private val metrics = mock[CacheMetrics]
  protected val cache = new CacheManager(cacheApi, metrics)

  protected def mockWsClient(wsClient: WSClient, url: String, httpResponseCode: Int, responseJson: String = "{}") = {
    val wsRequest = mock[WSRequest]
    val wsResponse = mock[WSResponse]

    when(cacheApi.get(url)).thenReturn(None)
    when(wsClient.url(url)).thenReturn(wsRequest)
    when(wsRequest.get()).thenReturn(successful(wsResponse))

    when(wsResponse.status).thenReturn(httpResponseCode)

    if (Range(200, 299).contains(httpResponseCode))
      when(wsResponse.json).thenReturn(parse(responseJson))
  }

}
