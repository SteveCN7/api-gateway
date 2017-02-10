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
import org.mockito.Mockito.when
import play.api.mvc.Results._
import uk.gov.hmrc.apigateway.service.ProxyService
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import play.api.http.Status._
import play.api.libs.json.Json._
import uk.gov.hmrc.apigateway.exception.GatewayError.ServerError
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._

import scala.concurrent.Future.{failed, successful}

class ProxyControllerSpec extends UnitSpec with MockitoSugar {

  private implicit val materializer = mock[Materializer]

  private trait Setup {
    val request = FakeRequest("POST", "/hello/world")
    val mockProxyService = mock[ProxyService]
    val underTest = new ProxyController(mockProxyService)
  }

  "proxy" should {

    "propagate the response" in new Setup {
      when(mockProxyService.proxy(any())).thenReturn(successful(NotFound(toJson("Item Not Found"))))

      val result = await(underTest.proxy(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe toJson("Item Not Found")
    }

    "convert exceptions to `InternalServerError` " in new Setup {
      when(mockProxyService.proxy(any())).thenReturn(failed(new RuntimeException))

      val result = await(underTest.proxy(request))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe toJson( ServerError())
    }

    "convert [502|503|504] responses" in new Setup {
      for (s <- List(BadGateway, ServiceUnavailable, GatewayTimeout)) {
        when(mockProxyService.proxy(any())).thenReturn(successful(s))

        val result = await(underTest.proxy(request))

        status(result) shouldBe SERVICE_UNAVAILABLE
        jsonBodyOf(result) shouldBe toJson("Service unavailable")
      }
    }

    "convert 501 responses" in new Setup {
      when(mockProxyService.proxy(any())).thenReturn(successful(NotImplemented))

      val result = await(underTest.proxy(request))

      status(result) shouldBe NOT_IMPLEMENTED
      jsonBodyOf(result) shouldBe toJson("API has not been implemented")
    }

  }

}
