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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.connector.impl.ProxyConnector
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.apigateway.util.RequestTags._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class ProxyConnectorSpec extends UnitSpec with WithFakeApplication with BeforeAndAfterEach {

  val stubPort = sys.env.getOrElse("WIREMOCK", "22220").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  trait Setup {
    val underTest = fakeApplication.injector.instanceOf[ProxyConnector]
  }

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  "proxy" should {

    val request = FakeRequest("GET", "/hello/world")

    "Proxy the request" in new Setup {
      givenGetReturns("/world", OK)

      val result = await(underTest.proxy(request, s"$wireMockUrl/world"))

      status(result) shouldBe OK
    }

    "Proxy the body" in new Setup {
      val body = """{"content":"body"}"""
      val requestWithBody = FakeRequest("POST", "/hello/world").withBody(AnyContentAsJson(Json.parse(body)))

      givenPostReturns("/world", OK)

      val result = await(underTest.proxy(requestWithBody, s"$wireMockUrl/world"))

      verify(postRequestedFor(urlEqualTo("/world")).withRequestBody(equalTo(body)))
    }

    "Forward the headers to the microservice" in new Setup {
      val requestWithHeader = request.withHeaders("aHeader" -> "aHeaderValue")

      givenGetReturns("/world", OK)

      await(underTest.proxy(requestWithHeader, s"$wireMockUrl/world"))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withHeader("aHeader", equalTo("aHeaderValue")))
    }

    "Not forward the Host header from the original request to the microservice" in new Setup {
      val requestWithHeader = request.withHeaders("Host" -> "api-gateway.service")

      givenGetReturns("/world", OK)

      await(underTest.proxy(requestWithHeader, s"$wireMockUrl/world"))

      verify(getRequestedFor(urlEqualTo("/world")).withHeader("Host", equalTo(s"localhost:$stubPort")))
    }

    val extraHeadersToPropagate = Table(
      ( "tag",                              "header",                       "value"                         ),
      ( AUTH_AUTHORIZATION,                 "Authorization",                "Bearer 12345"                  ),
      ( CLIENT_ID,                          "X-Client-ID",                  "123456"                        ),
      ( REQUEST_TIMESTAMP_NANO,             "X-Request-Timestamp",          "1232356"                       )
    )

    "Add extra headers in the request" in new Setup {

      forAll(extraHeadersToPropagate) { (tag, header, value) =>
        val requestWithTag = request.copyFakeRequest(tags = Map(tag -> value))

        givenGetReturns("/world", OK)

        await(underTest.proxy(requestWithTag, s"$wireMockUrl/world"))

        verify(getRequestedFor(urlEqualTo("/world"))
          .withHeader(header, equalTo(value)))
      }
    }

    "Add Oauth token header in the request" in new Setup {
      val requestWithTag = request.copyFakeRequest(tags = Map(OAUTH_AUTHORIZATION -> "Bearer 1234567"))

      givenGetReturns("/world", OK)

      await(underTest.proxy(requestWithTag, s"$wireMockUrl/world"))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withHeader(X_CLIENT_AUTHORIZATION_TOKEN, equalTo("1234567")))
    }

    "Not include extra headers when there is no tag in the request" in new Setup {

      val requestWithoutTags = request.copyFakeRequest(tags = Map())

      await(underTest.proxy(requestWithoutTags, s"$wireMockUrl/world"))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withoutHeader("Authorization")
        .withoutHeader("X-Client-ID")
        .withoutHeader("X-Client-Authorization-Token")
        .withoutHeader("X-Request-Timestamp"))
    }
  }

  def givenGetReturns(endpoint: String, status: Int) = {
    stubFor(get(urlEqualTo(endpoint))
      .willReturn(aResponse().withStatus(status)))
  }

  def givenPostReturns(endpoint: String, status: Int) = {
    stubFor(get(urlEqualTo(endpoint))
      .willReturn(aResponse().withStatus(status)))
  }

}
