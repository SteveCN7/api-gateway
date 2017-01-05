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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.Json._
import uk.gov.hmrc.apigateway.connector.impl.ThirdPartyApplicationConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.NotFound
import uk.gov.hmrc.apigateway.model.Application
import uk.gov.hmrc.apigateway.play.binding.PlayBindings.applicationFormat
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class ThirdPartyApplicationConnectorSpec extends UnitSpec with BeforeAndAfterEach with WithFakeApplication {

  val stubPort = sys.env.getOrElse("WIREMOCK", "22223").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  val serverTokenHeader = "X-server-token"
  val application = Application(UUID.randomUUID(), "App Name")

  trait Setup {
    val underTest = fakeApplication.injector.instanceOf[ThirdPartyApplicationConnector]
  }

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  "Third party application connector" should {
    val serverToken = "31c99f9482de49544c6cc3374c378028"

    "retrieve an application by server token" in new Setup {
      stubFor(get(urlPathEqualTo(s"/application"))
        .withHeader(serverTokenHeader, equalTo(serverToken))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(stringify(toJson(application)))
        ))

      await(underTest.getByServerToken(serverToken)) shouldBe application
    }

    "propagate an exception when the application cannot be fetched" in new Setup {
      stubFor(get(urlPathEqualTo("/application"))
        .withHeader(serverTokenHeader, equalTo(serverToken))
        .willReturn(
          aResponse().withStatus(404)
        ))

      intercept[NotFound] {
        await(underTest.getByServerToken(serverToken))
      }
    }
  }
}
