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

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json._
import play.api.mvc.Results._
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.model.ApiRequest
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._
import uk.gov.hmrc.apigateway.service.{AuditService, ProxyService, RoutingService}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.{failed, successful}

class ProxyControllerSpec extends UnitSpec with MockitoSugar {

  private implicit val materializer = mock[Materializer]

  private trait Setup {
    val request = FakeRequest("POST", "/hello/world")
    val apiRequest = mock[ApiRequest]

    val proxyService = mock[ProxyService]
    val auditService = mock[AuditService]
    val routingService = mock[RoutingService]
    when(routingService.routeRequest(any())).thenReturn(apiRequest)

    val proxyController = new ProxyController(proxyService, routingService, auditService)
  }

  "proxy" should {

    "propagate the response" in new Setup {
      when(proxyService.proxy(any(), any())).thenReturn(successful(NotFound(toJson("Item Not Found"))))

      val result = await(proxyController.proxy(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe toJson("Item Not Found")

      verifyZeroInteractions(auditService)
    }

    "convert exceptions to `InternalServerError` " in new Setup {
      when(proxyService.proxy(any(), any())).thenReturn(failed(new RuntimeException))

      val result = await(proxyController.proxy(request))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe toJson(GatewayError.ServerError())

      verifyZeroInteractions(auditService)
    }

    "convert [502|503|504] responses" in new Setup {
      for (s <- List(BadGateway, ServiceUnavailable, GatewayTimeout)) {
        when(proxyService.proxy(any(), any())).thenReturn(successful(s))

        val result = await(proxyController.proxy(request))

        status(result) shouldBe SERVICE_UNAVAILABLE
        jsonBodyOf(result) shouldBe toJson(GatewayError.ServiceUnavailable())

        verifyZeroInteractions(auditService)
      }
    }

    "convert 501 responses" in new Setup {
      when(proxyService.proxy(any(), any())).thenReturn(successful(NotImplemented))

      val result = await(proxyController.proxy(request))

      status(result) shouldBe NOT_IMPLEMENTED
      jsonBodyOf(result) shouldBe toJson(GatewayError.NotImplemented())

      verifyZeroInteractions(auditService)
    }

    "audit `MissingCredentials` failures" in new Setup {
      when(proxyService.proxy(any(), any())).thenReturn(failed(GatewayError.MissingCredentials(request, apiRequest)))

      val result = await(proxyController.proxy(request))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe toJson(GatewayError.MissingCredentials(request, apiRequest))

      verify(auditService).auditFailingRequest(request, apiRequest)
    }

    "audit `InvalidCredentials` failures" in new Setup {
      when(proxyService.proxy(any(), any())).thenReturn(failed(GatewayError.InvalidCredentials(request, apiRequest)))

      val result = await(proxyController.proxy(request))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe toJson(GatewayError.InvalidCredentials(request, apiRequest))

      verify(auditService).auditFailingRequest(request, apiRequest)
    }

  }

}
