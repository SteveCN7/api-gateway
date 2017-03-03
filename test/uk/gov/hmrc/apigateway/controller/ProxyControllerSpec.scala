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

package uk.gov.hmrc.apigateway.controller

import java.util.UUID

import akka.stream.Materializer
import com.google.common.net.{HttpHeaders => http}
import it.uk.gov.hmrc.apigateway.testutils.RequestUtils
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json._
import play.api.mvc.Results._
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.exception.GatewayError.MatchingResourceNotFound
import uk.gov.hmrc.apigateway.model.ApiRequest
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._
import uk.gov.hmrc.apigateway.service.{ProxyService, RoutingService}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.{failed, successful}

class ProxyControllerSpec extends UnitSpec with MockitoSugar with RequestUtils {

  private implicit val materializer = mock[Materializer]

  private trait Setup {
    val request = FakeRequest("POST", "/hello/world")
    val mockProxyService = mock[ProxyService]
    val mockRoutingService = mock[RoutingService]
    val underTest = new ProxyController(mockProxyService, mockRoutingService)
    val requestId = UUID.randomUUID().toString

    when(mockRoutingService.routeRequest(any())).thenReturn(mock[ApiRequest])
  }

  "proxy" should {

    "propagate a downstream successful response" in new Setup {
      when(mockProxyService.proxy(any(), any())(any())).thenReturn(successful(Ok(toJson("""{"foo":"bar"}"""))))

      val result = await(underTest.proxy()(requestId)(request))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe toJson("""{"foo":"bar"}""")
      validateHeaders(result.header.headers, (X_REQUEST_ID, None))
    }

    "propagate a downstream error response" in new Setup {
      when(mockProxyService.proxy(any(), any())(any())).thenReturn(successful(NotFound(toJson("Item Not Found"))))

      val result = await(underTest.proxy()(any())(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe toJson("Item Not Found")
      validateHeaders(result.header.headers, (X_REQUEST_ID, None))
    }

    "convert exceptions to `InternalServerError` " in new Setup {
      when(mockProxyService.proxy(any(), any())(any())).thenReturn(failed(new RuntimeException))

      val result = await(underTest.proxy()(requestId)(request))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe toJson(GatewayError.ServerError())
    }

    "convert [502|503|504] responses" in new Setup {
      for (s <- List(BadGateway, ServiceUnavailable, GatewayTimeout)) {
        when(mockProxyService.proxy(any(), any())(any())).thenReturn(successful(s))

        val result = await(underTest.proxy()(requestId)(request))

        status(result) shouldBe SERVICE_UNAVAILABLE
        jsonBodyOf(result) shouldBe toJson(GatewayError.ServiceUnavailable())
      }
    }

    "convert 501 responses" in new Setup {
      when(mockProxyService.proxy(any(), any())(any())).thenReturn(successful(NotImplemented))

      val result = await(underTest.proxy()(requestId)(request))

      status(result) shouldBe NOT_IMPLEMENTED
      jsonBodyOf(result) shouldBe toJson(GatewayError.NotImplemented())
    }

    "Align with WSO2 response headers when request fails before it gets proxied" in new Setup {
      when(mockRoutingService.routeRequest(any())).thenReturn(failed(MatchingResourceNotFound()))

      val result = await(underTest.proxy()(requestId)(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe toJson(MatchingResourceNotFound())
      validateHeaders(result.header.headers,
        (X_REQUEST_ID, Some(requestId)),
        (CACHE_CONTROL, Some("no-cache")),
        (CONTENT_TYPE, Some("application/json; charset=UTF-8")),
        (http.X_FRAME_OPTIONS, None),
        (http.X_CONTENT_TYPE_OPTIONS, None))
    }
  }
}
