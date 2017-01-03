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
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.Json._
import uk.gov.hmrc.apigateway.connector.impl.DelegatedAuthorityConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.InvalidCredentials
import uk.gov.hmrc.apigateway.model.{Authority, ThirdPartyDelegatedAuthority, Token}
import uk.gov.hmrc.apigateway.play.binding.PlayBindings.authorityFormat
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}

class DelegatedAuthorityConnectorSpec extends UnitSpec with BeforeAndAfterEach with WithFakeApplication {

  val stubPort = sys.env.getOrElse("WIREMOCK", "22222").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  val authority = Authority(ThirdPartyDelegatedAuthority("sandbox_token", "uKQXtOfZEmW8z5UOwHsg3ANF_fwa", Token("accessToken", Set.empty, DateTime.now)))

  trait Setup {
    val underTest = fakeApplication.injector.instanceOf[DelegatedAuthorityConnector]
  }

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  "Delegated authority connector" should {
    val accessToken = "31c99f9482de49544c6cc3374c378028"

    "throw an exception when access token is invalid" in new Setup {

      stubFor(get(urlPathEqualTo("/authority"))
        .withQueryParam("access_token", equalTo(accessToken))
        .willReturn(
          aResponse().withStatus(404)
      ))

      intercept[InvalidCredentials] {
        await(underTest.getByAccessToken(accessToken))
      }
    }

    "return the delegated authority when access token is valid" in new Setup {

      stubFor(get(urlPathEqualTo(s"/authority"))
        .withQueryParam("access_token", equalTo(accessToken))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(stringify(toJson(authority)))
        ))

      await(underTest.getByAccessToken(accessToken)) shouldBe authority
    }

  }

}
