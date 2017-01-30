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
import org.scalatest.prop.Tables.Table
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.connector.impl.ProxyConnector
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import org.scalatest.prop.TableDrivenPropertyChecks.forAll

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

      givenTheUrlReturns("/world", OK)

      val result = await(underTest.proxy(request, s"$wireMockUrl/world"))

      status(result) shouldBe OK
    }

    val headersToPropagate = Table(
      ( "tag",                              "header",                       "value"                         ),
      ( ACCEPT,                             "Accept",                       "application/vnd.hmrc.1.0+json" ),
      ( AUTHORIZATION,                      "Authorization",                "Bearer 12345"                  ),
      ( X_API_GATEWAY_CLIENT_ID,            "X-Client-ID",                  "123456"                        ),
      ( X_API_GATEWAY_AUTHORIZATION_TOKEN,  "X-Client-Authorization-Token", "78910"                         ),
      ( X_API_GATEWAY_REQUEST_TIMESTAMP,    "X-Request-Timestamp",          "1232356"                       )
    )

    "Add headers in the request" in new Setup {

      forAll(headersToPropagate) { (tag, header, value) =>
        val requestWithTag = request.copyFakeRequest(tags = Map(tag -> value))

        givenTheUrlReturns("/world", OK)

        await(underTest.proxy(requestWithTag, s"$wireMockUrl/world"))

        verify(getRequestedFor(urlEqualTo("/world"))
          .withHeader(header, equalTo(value)))
      }
    }

    "Not include headers when there is no tag in the request" in new Setup {

      val requestWithoutTags = request.copyFakeRequest(tags = Map())

      await(underTest.proxy(requestWithoutTags, s"$wireMockUrl/world"))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withoutHeader("Authorization")
        .withoutHeader("X-Client-ID")
        .withoutHeader("X-Client-Authorization-Token")
        .withoutHeader("X-Request-Timestamp"))
    }
  }

  def givenTheUrlReturns(endpoint: String, status: Int) = {
    stubFor(get(urlEqualTo(endpoint))
      .willReturn(aResponse().withStatus(status)))
  }
}
