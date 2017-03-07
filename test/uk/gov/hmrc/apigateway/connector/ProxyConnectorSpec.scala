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

import java.util.concurrent.TimeoutException

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.connector.impl.ProxyConnector
import uk.gov.hmrc.apigateway.model.{ApiIdentifier, ApiRequest}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.duration._

class ProxyConnectorSpec extends UnitSpec with WithFakeApplication with BeforeAndAfterEach {

  implicit override val defaultTimeout = 45.seconds

  private val stubPort = sys.env.getOrElse("WIREMOCK", "22220").toInt
  private val stubHost = "localhost"
  private val wireMockUrl = s"http://$stubHost:$stubPort"
  private val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
//
//  implicit private val maximumWaitingTime = 45.seconds

  trait Setup {
    val underTest = fakeApplication.injector.instanceOf[ProxyConnector]
  }

  val apiRequest = ApiRequest(
    timeInNanos = Some(1232356),
    apiIdentifier = ApiIdentifier("c", "v"),
    apiEndpoint = s"$wireMockUrl/world",
    clientId = Some("123456"),
    bearerToken = Some("Bearer 12345"))

  override def beforeEach {
    // wireMockServer.resetMappings()
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  "proxy" should {

    val request = FakeRequest("GET", "/hello/world")

    "failing with a `TimeoutException` when there is a delay of 30 seconds" in new Setup {
      givenGetReturns("/world", OK, delay = 30000)

      intercept[TimeoutException] {
        await(underTest.proxy(request, apiRequest))// (timeout = defaultTimeout)
      }
    }

    "Proxy the request when the response is processed in less than 30 seconds" in new Setup {
      // TODO: this is failing if using delay >= 20 sec [maybe hardcoded somewhere in WireMock!?] => test it with `SBT RUN`
      givenGetReturns("/world", OK, delay = 23000)

      val result = await(underTest.proxy(request, apiRequest))// (timeout = defaultTimeout)

      status(result) shouldBe OK
    }

    "Proxy the body" in new Setup {
      val body = """{"content":"body"}"""
      val requestWithBody = FakeRequest("POST", "/hello/world").withBody(AnyContentAsJson(Json.parse(body)))

      givenPostReturns("/world", OK)

      val result = await(underTest.proxy(requestWithBody, apiRequest))

      verify(postRequestedFor(urlEqualTo("/world")).withRequestBody(equalTo(body)))
    }

    "Forward the headers to the microservice" in new Setup {
      val requestWithHeader = request.withHeaders("aHeader" -> "aHeaderValue")

      givenGetReturns("/world", OK)

      await(underTest.proxy(requestWithHeader, apiRequest))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withHeader("aHeader", equalTo("aHeaderValue")))
    }

    "Not forward the Host header from the original request to the microservice" in new Setup {
      val requestWithHeader = request.withHeaders("Host" -> "api-gateway.service")

      givenGetReturns("/world", OK)

      await(underTest.proxy(requestWithHeader, apiRequest))

      verify(getRequestedFor(urlEqualTo("/world")).withHeader("Host", equalTo(s"localhost:$stubPort")))
    }

    val gatewayHeaders = Map(
      "Authorization" -> apiRequest.bearerToken.get,
      "X-Client-ID" -> apiRequest.clientId.get,
      "X-Request-Timestamp" -> apiRequest.timeInNanos.get.toString)

    "Add extra headers in the request" in new Setup {

      for ((header, value) <- gatewayHeaders) {

        givenGetReturns("/world", OK)

        await(underTest.proxy(request, apiRequest))

        verify(getRequestedFor(urlEqualTo("/world"))
          .withHeader(header, equalTo(value)))
      }
    }

    "Override the extra headers from the original request" in new Setup {

      for ((header, value) <- gatewayHeaders) {

        val requestWithHeader = request.withHeaders(header -> "originalRequestHeader")

        givenGetReturns("/world", OK)

        await(underTest.proxy(requestWithHeader, apiRequest))

        verify(getRequestedFor(urlEqualTo("/world")).withHeader(header, equalTo(value)))
      }
    }

    "Add Oauth token header in the request" in new Setup {
      givenGetReturns("/world", OK)

      await(underTest.proxy(request, apiRequest))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withHeader(X_CLIENT_AUTHORIZATION_TOKEN, equalTo("12345")))
    }

    "Not include extra headers when there is no tag in the request" in new Setup {

      await(underTest.proxy(request, apiRequest.copy(
        timeInNanos = None,
        clientId = None,
        bearerToken = None)))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withoutHeader("Authorization")
        .withoutHeader("X-Client-ID")
        .withoutHeader("X-Client-Authorization-Token")
        .withoutHeader("X-Request-Timestamp"))
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
