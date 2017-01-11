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
import it.uk.gov.hmrc.apigateway.stubs.ThirdPartyApplicationStubMappings
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.apigateway.connector.impl.ThirdPartyApplicationConnector
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class ThirdPartyApplicationConnectorSpec extends UnitSpec with BeforeAndAfterEach with WithFakeApplication with ThirdPartyApplicationStubMappings {

  val stubPort = sys.env.getOrElse("WIREMOCK", "22223").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  val applicationId = UUID.randomUUID()
  val application = Application(applicationId, "App Name")

  val v1 = Version("1.0")
  val v2 = Version("2.0")
  val subscriptions = Seq(
    API(context = "c1", versions = Seq(Subscription(v1, subscribed = true), Subscription(v2, subscribed = false))),
    API(context = "c2", versions = Seq(Subscription(v1, subscribed = false), Subscription(v2, subscribed = true))))

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

  "getApplicationByServerToken" should {

    val serverToken = "31c99f9482de49544c6cc3374c378028"

    "propagate the exception when the application cannot be fetched by server token" in new Setup {
      stubFor(willNotFindAnyApplicationForServerToken(serverToken))
      intercept[NotFound] {
        await(underTest.getApplicationByServerToken(serverToken))
      }
    }

    "return the application when the server token is valid" in new Setup {
      stubFor(returnTheApplicationForServerToken(serverToken, application))
      await(underTest.getApplicationByServerToken(serverToken)) shouldBe application
    }

  }

  "getApplicationByClientId" should {

    val clientId = "aoihefiwohg93hg9ueirgnvoenvl"

    "propagate the exception when the application cannot be fetched by client id" in new Setup {
      stubFor(willNotFindAnyApplicationForClientId(clientId))
      intercept[NotFound] {
        await(underTest.getApplicationByClientId(clientId))
      }
    }

    "return the application when the client id is valid" in new Setup {
      stubFor(returnTheApplicationForClientId(clientId, application))
      await(underTest.getApplicationByClientId(clientId)) shouldBe application
    }
  }

  "getSubscriptionsByApplicationId" should {

    "propagate the exception when the subscriptions cannot be fetched" in new Setup {
      stubFor(willNotFindAnySubscriptionForApplicationId(applicationId.toString))
      intercept[NotFound] {
        await(underTest.getSubscriptionsByApplicationId(applicationId.toString))
      }
    }

    "return the subscriptions when the application id is valid" in new Setup {
      stubFor(returnTheSubscriptionsForApplicationId(applicationId.toString, subscriptions))
      await(underTest.getSubscriptionsByApplicationId(applicationId.toString)) shouldBe subscriptions
    }

  }

}
