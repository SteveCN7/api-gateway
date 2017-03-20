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

package uk.gov.hmrc.apigateway.connector

import java.util.UUID
import java.util.concurrent.TimeoutException

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import it.uk.gov.hmrc.apigateway.testutils.RequestUtils
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.config.AppContext
import uk.gov.hmrc.apigateway.connector.impl.ProxyConnector
import uk.gov.hmrc.apigateway.model.{ApiIdentifier, ApiRequest}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import com.google.common.net.{HttpHeaders => http}

class ProxyConnectorSpec extends UnitSpec with WithFakeApplication with MockitoSugar with BeforeAndAfterEach with RequestUtils {

  private val stubPort = sys.env.getOrElse("WIREMOCK", "22220").toInt
  private val stubHost = "localhost"
  private val wireMockUrl = s"http://$stubHost:$stubPort"
  private val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  trait Setup {
    val appContext = mock[AppContext]
    val requestId = UUID.randomUUID().toString
    val underTest = new ProxyConnector(wsClient = fakeApplication.injector.instanceOf[WSClient], appContext)
  }

  val apiRequest = ApiRequest(
    timeInNanos = Some(33333),
    apiIdentifier = ApiIdentifier("c", "v"),
    apiEndpoint = s"$wireMockUrl/world",
    clientId = Some("7777"),
    authBearerToken = Some("Bearer 666"))

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  "proxy" should {

    val request = FakeRequest("GET", "/hello/world")

    "Have a connect timeout configuration of 5 seconds" in new Setup {

      fakeApplication.configuration.getInt("play.ws.timeout.connection") shouldBe Some(5000)
    }

    "Fail with a `TimeoutException` when the downstream service response is too slow" in new Setup {

      when(appContext.requestTimeoutInMilliseconds).thenReturn(10)

      givenGetReturns("/world", OK, delay = 50)

      intercept[TimeoutException] {
        await(underTest.proxy(request, apiRequest)(requestId))
      }
    }

    "Proxy the request when the downstream service response is processed on time" in new Setup {

      when(appContext.requestTimeoutInMilliseconds).thenReturn(50)

      givenGetReturns("/world", OK, delay = 10)

      val result = await(underTest.proxy(request, apiRequest)(requestId))

      status(result) shouldBe OK
    }

    "Proxy the body" in new Setup {
      val body = """{"content":"body"}"""
      val requestWithBody = FakeRequest("POST", "/hello/world").withBody(AnyContentAsJson(Json.parse(body)))

      givenPostReturns("/world", OK)

      await(underTest.proxy(requestWithBody, apiRequest)(requestId))

      verify(postRequestedFor(urlEqualTo("/world")).withRequestBody(equalTo(body)))
    }

    "Forward the headers to the microservice" in new Setup {
      val requestWithHeader = request.withHeaders("aHeader" -> "aHeaderValue")

      givenGetReturns("/world", OK)

      await(underTest.proxy(requestWithHeader, apiRequest)(requestId))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withHeader("aHeader", equalTo("aHeaderValue"))
        .withHeader(X_REQUEST_ID, equalTo(requestId)))
    }

    "Not forward the Host header from the original request to the microservice" in new Setup {
      val requestWithHeader = request.withHeaders("Host" -> "api-gateway.service")

      givenGetReturns("/world", OK)

      await(underTest.proxy(requestWithHeader, apiRequest)(requestId))

      verify(getRequestedFor(urlEqualTo("/world")).withHeader("Host", equalTo(s"localhost:$stubPort")))
    }

    val gatewayHeaders = Map(
      "Authorization" -> apiRequest.authBearerToken.get,
      "X-Client-ID" -> apiRequest.clientId.get,
      "X-Request-Timestamp" -> apiRequest.timeInNanos.get.toString)

    "Add extra headers in the request" in new Setup {

      for ((header, value) <- gatewayHeaders) {

        givenGetReturns("/world", OK)

        await(underTest.proxy(request, apiRequest)(requestId))

        verify(getRequestedFor(urlEqualTo("/world"))
          .withHeader(header, equalTo(value)))
      }
    }

    "Override the extra headers from the original request" in new Setup {

      for ((header, value) <- gatewayHeaders) {

        val requestWithHeader = request.withHeaders(header -> "originalRequestHeader")

        givenGetReturns("/world", OK)

        await(underTest.proxy(requestWithHeader, apiRequest)(requestId))

        verify(getRequestedFor(urlEqualTo("/world")).withHeader(header, equalTo(value)))
      }
    }

    "Add Oauth token header in the request" in new Setup {
      givenGetReturns("/world", OK)

      await(underTest.proxy(request, apiRequest)(requestId))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withHeader(X_CLIENT_AUTHORIZATION_TOKEN, equalTo("666")))
    }

    "Not include extra headers when there is no tag in the request" in new Setup {

      await(underTest.proxy(request, apiRequest.copy(
        timeInNanos = None,
        clientId = None,
        authBearerToken = None))(requestId))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withoutHeader("Authorization")
        .withoutHeader("X-Client-ID")
        .withoutHeader("X-Client-Authorization-Token")
        .withoutHeader("X-Request-Timestamp"))
    }

    "Align with WSO2 response headers" in new Setup {
      givenGetReturns("/world", OK)

      val result = await(underTest.proxy(request, apiRequest)(requestId))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withHeader(X_CLIENT_AUTHORIZATION_TOKEN, equalTo("666")))

      validateHeaders(result.header.headers,
        (TRANSFER_ENCODING, Some("chunked")),
        (VARY, Some("Accept")),
        (CONTENT_LENGTH, None),
        (http.STRICT_TRANSPORT_SECURITY, None),
        (http.X_FRAME_OPTIONS, None),
        (http.X_CONTENT_TYPE_OPTIONS, None))
    }
  }

  def givenGetReturns(endpoint: String, status: Int, delay: Int = 0) = {
    stubFor(
      get(urlEqualTo(endpoint))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withFixedDelay(delay)
        )
    )
  }

  def givenPostReturns(endpoint: String, status: Int, delay: Int = 0) = {
    stubFor(
      post(urlEqualTo(endpoint))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withFixedDelay(delay)
      )
    )
  }

}
