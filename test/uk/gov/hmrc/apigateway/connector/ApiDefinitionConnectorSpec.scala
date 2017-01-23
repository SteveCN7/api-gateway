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
import it.uk.gov.hmrc.apigateway.stubs.ApiDefinitionStubMappings
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.apigateway.connector.impl.ApiDefinitionConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.NotFound
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class ApiDefinitionConnectorSpec extends UnitSpec with BeforeAndAfterEach with WithFakeApplication with ApiDefinitionStubMappings {

  val stubPort = sys.env.getOrElse("WIREMOCK", "22221").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  trait Setup {
    val underTest = fakeApplication.injector.instanceOf[ApiDefinitionConnector]
  }

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  "getByContext" should {

    "return the api definition" in new Setup {
      val apiDefinition = ApiDefinition("hello", "http://hello.service",
        Seq(ApiVersion("1.0", Seq(ApiEndpoint("/world", "GET", AuthType.NONE)))))

      stubFor(returnTheApiDefinition(apiDefinition))

      await(underTest.getByContext("hello")) shouldBe apiDefinition
    }

    "propagate the exception when access token is invalid" in new Setup {
      stubFor(notReturnAnApiDefinitionForContext("hello"))

      intercept[NotFound] {
        await(underTest.getByContext("hello"))
      }
    }
  }

}
