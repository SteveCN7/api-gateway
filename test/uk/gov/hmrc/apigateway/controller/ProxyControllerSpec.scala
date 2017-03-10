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
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.exception.GatewayError.InvalidCredentials
import uk.gov.hmrc.apigateway.model.ApiRequest
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._
import uk.gov.hmrc.apigateway.service.{AuditService, ProxyService, RoutingService}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class ProxyControllerSpec extends UnitSpec with MockitoSugar with RequestUtils {

  private implicit val materializer = mock[Materializer]

  private trait Setup {
    val request = FakeRequest("POST", "/hello/world")
    val apiRequest = mock[ApiRequest]
    val requestId = UUID.randomUUID().toString

    val proxyService = mock[ProxyService]
    def mockProxyService(result: Future[Result]) = {
      when(proxyService.proxy(request, apiRequest)(requestId)).thenReturn(result)
    }

    val routingService = mock[RoutingService]
    when(routingService.routeRequest(request)).thenReturn(apiRequest)

    val auditService = mock[AuditService]

    val proxyController = new ProxyController(proxyService, routingService, auditService)
  }

  "proxy" should {

    "propagate a downstream successful response" in new Setup {
      mockProxyService(successful(Ok(toJson("""{"foo":"bar"}"""))))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe toJson("""{"foo":"bar"}""")
      validateHeaders(result.header.headers, (X_REQUEST_ID, None))

      verifyZeroInteractions(auditService)
    }

    "propagate a downstream error response" in new Setup {
      mockProxyService(successful(NotFound(toJson("Item Not Found"))))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe toJson("Item Not Found")
      validateHeaders(result.header.headers, (X_REQUEST_ID, None))

      verifyZeroInteractions(auditService)
    }

    "convert exceptions to `InternalServerError` " in new Setup {
      mockProxyService(failed(new RuntimeException))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe toJson(GatewayError.ServerError())

      verifyZeroInteractions(auditService)
    }

    "convert [502|503|504] responses" in new Setup {
      for (s <- List(BadGateway, ServiceUnavailable, GatewayTimeout)) {
        mockProxyService(successful(s))

        val result = await(proxyController.proxy()(requestId)(request))

        status(result) shouldBe SERVICE_UNAVAILABLE
        jsonBodyOf(result) shouldBe toJson(GatewayError.ServiceNotAvailable())

        verifyZeroInteractions(auditService)
      }
    }

    "convert 501 responses" in new Setup {
      mockProxyService(successful(NotImplemented))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe NOT_IMPLEMENTED
      jsonBodyOf(result) shouldBe toJson(GatewayError.NotImplemented())

      verifyZeroInteractions(auditService)
    }

    "audit `MissingCredentials` failures" in new Setup {
      mockProxyService(failed(GatewayError.MissingCredentials(request, apiRequest)))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe toJson(GatewayError.MissingCredentials(request, apiRequest))

      verify(auditService).auditFailingRequest(request, apiRequest)(requestId)
    }

    "audit `InvalidCredentials` failures" in new Setup {
      mockProxyService(failed(GatewayError.InvalidCredentials(request, apiRequest)))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe toJson(GatewayError.InvalidCredentials(request, apiRequest))

      verify(auditService).auditFailingRequest(request, apiRequest)(requestId)
    }

    "Align with WSO2 response headers when request fails before it gets proxied" in new Setup {
      mockProxyService(failed(GatewayError.MatchingResourceNotFound()))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe toJson(GatewayError.MatchingResourceNotFound())
      validateHeaders(result.header.headers,
        (X_REQUEST_ID, Some(requestId)),
        (CACHE_CONTROL, Some("no-cache")),
        (CONTENT_TYPE, Some("application/json; charset=UTF-8")),
        (http.X_FRAME_OPTIONS, None),
        (http.X_CONTENT_TYPE_OPTIONS, None))
    }

    "return 401 with 'WWW-Authenticate' header when InvalidCredentials is thrown" in new Setup {
      when(routingService.routeRequest(any())).thenReturn(failed(InvalidCredentials(request, apiRequest)))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe UNAUTHORIZED

      jsonBodyOf(result) shouldBe Json.obj("code" -> "INVALID_CREDENTIALS", "message" -> "Invalid Authentication information provided")
      result.header.headers(WWW_AUTHENTICATE) shouldBe """Bearer realm="HMRC API Platform""""
    }
  }
}
