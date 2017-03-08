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

package uk.gov.hmrc.apigateway.service

import java.util.UUID

import com.google.common.net.HttpHeaders._
import org.mockito.BDDMockito.given
import org.mockito.Mockito.{timeout, verify, verifyZeroInteractions}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.mvc.Http.MimeTypes.JSON
import uk.gov.hmrc.apigateway.connector.impl.ProxyConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.ServiceNotAvailable
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.model.{ApiIdentifier, ApiRequest}
import uk.gov.hmrc.play.test.UnitSpec

class ProxyServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  trait Setup {
    val request = FakeRequest("GET", "/hello/world")
    val apiRequest = ApiRequest(
      apiIdentifier = mock[ApiIdentifier],
      authType = USER,
      apiEndpoint = "http://hello-world.service/hello/world",
      scope = Some("scope"))

    val proxyConnector = mock[ProxyConnector]
    val auditService = mock[AuditService]
    val underTest = new ProxyService(proxyConnector, auditService)
    val requestId = UUID.randomUUID().toString
  }

  "proxy" should {

    "call and return the response from the microservice" in new Setup {
      val response = Ok("hello")

      given(proxyConnector.proxy(request, apiRequest)(requestId)).willReturn(response)

      val result = await(underTest.proxy(request, apiRequest)(requestId))

      result shouldBe response
    }

    "audit the request" in new Setup {

      val response = Ok("hello")

      given(proxyConnector.proxy(request, apiRequest)(requestId)).willReturn(response)

      await(underTest.proxy(request, apiRequest)(requestId))

      verify(auditService, timeout(2000)).auditSuccessfulRequest(request, apiRequest, response)
    }

    "not audit the request for open endpoint" in new Setup {
      val openApiRequest = apiRequest.copy(authType = NONE)

      given(proxyConnector.proxy(request, openApiRequest)(requestId)).willReturn(Ok("hello"))

      await(underTest.proxy(request, openApiRequest)(requestId))

      verifyZeroInteractions(auditService)
    }

    "fail a POST request without a Content-Type header" in new Setup {
      val postRequest = FakeRequest("POST", "/hello/world").withJsonBody(Json.toJson("""{"foo":"bar"}"""))
      val openApiRequest = apiRequest.copy(authType = NONE)

      intercept[ServiceNotAvailable] {
        await(underTest.proxy(postRequest, openApiRequest)(requestId))
      }
    }

    "fail a POST request without a body" in new Setup {
      val postRequest = FakeRequest("POST", "/hello/world").withHeaders((CONTENT_TYPE, JSON))
      val openApiRequest = apiRequest.copy(authType = NONE)

      intercept[ServiceNotAvailable] {
        await(underTest.proxy(postRequest, openApiRequest)(requestId))
      }
    }

    "fail a PUT request without a Content-Type header" in new Setup {
      val postRequest = FakeRequest("PUT", "/hello/world").withJsonBody(Json.toJson("""{"foo":"bar"}"""))
      val openApiRequest = apiRequest.copy(authType = NONE)

      intercept[ServiceNotAvailable] {
        await(underTest.proxy(postRequest, openApiRequest)(requestId))
      }
    }

    "fail a PUT request without a body" in new Setup {
      val postRequest = FakeRequest("PUT", "/hello/world").withHeaders((CONTENT_TYPE, JSON))
      val openApiRequest = apiRequest.copy(authType = NONE)

      intercept[ServiceNotAvailable] {
        await(underTest.proxy(postRequest, openApiRequest)(requestId))
      }
    }

    "fail a PATCH request without a Content-Type header" in new Setup {
      val postRequest = FakeRequest("PATCH", "/hello/world").withJsonBody(Json.toJson("""{"foo":"bar"}"""))
      val openApiRequest = apiRequest.copy(authType = NONE)

      intercept[ServiceNotAvailable] {
        await(underTest.proxy(postRequest, openApiRequest)(requestId))
      }
    }

    "fail a PATCH request without a body" in new Setup {
      val postRequest = FakeRequest("PATCH", "/hello/world").withHeaders((CONTENT_TYPE, JSON))
      val openApiRequest = apiRequest.copy(authType = NONE)

      intercept[ServiceNotAvailable] {
        await(underTest.proxy(postRequest, openApiRequest)(requestId))
      }
    }
  }
}
