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

import org.mockito.BDDMockito.given
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.apigateway.connector.impl.ThirdPartyApplicationConnector
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model.RateLimitTier.{SILVER, BRONZE}
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.repository.RateLimitRepository
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future._

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val serverToken = "serverToken"
    val applicationId = UUID.randomUUID()
    val clientId = "clientId"
    val application = Application(id = applicationId, clientId = "clientId", name = "App Name", rateLimitTier = BRONZE)
    val bronzeRateLimit = 5
    val silverRateLimit = 10

    val v1 = Version("1.0")
    val v2 = Version("2.0")
    val apis = Seq(
      Api(context = "c1", versions = Seq(Subscription(v1, subscribed = true), Subscription(v2, subscribed = false))),
      Api(context = "c2", versions = Seq(Subscription(v1, subscribed = false), Subscription(v2, subscribed = true))))
    val subscribedApi = ApiIdentifier("c1", "1.0")

    val applicationConnector = mock[ThirdPartyApplicationConnector]
    val rateLimitRepository = mock[RateLimitRepository]
    val configuration = mock[Configuration]
    val underTest = new ApplicationService(applicationConnector, rateLimitRepository, configuration)

    given(configuration.getInt("rateLimit.bronze")).willReturn(Some(bronzeRateLimit))
    given(configuration.getInt("rateLimit.silver")).willReturn(Some(silverRateLimit))
  }

  "Get application by server token" should {

    "return the application when an application exists for the given server token" in new Setup {
      when(applicationConnector.getApplicationByServerToken(serverToken)).thenReturn(successful(application))
      val result = await(underTest.getByServerToken(serverToken))
      result shouldBe application
    }

    "propagate the error when the application cannot be fetched for the given server token" in new Setup {
      when(applicationConnector.getApplicationByServerToken(serverToken)).thenReturn(failed(NotFound()))
      intercept[NotFound] {
        await(underTest.getByServerToken(serverToken))
      }
    }
  }

  "Get application by client id" should {

    "return the application when an application exists for the given client id" in new Setup {
      when(applicationConnector.getApplicationByClientId(clientId)).thenReturn(successful(application))
      val result = await(underTest.getByClientId(clientId))
      result shouldBe application
    }

    "propagate the error when the application cannot be fetched for the given client id" in new Setup {
      when(applicationConnector.getApplicationByClientId(clientId)).thenReturn(failed(NotFound()))
      intercept[NotFound] {
        await(underTest.getByClientId(clientId))
      }
    }
  }

  "validateSubscriptionAndRateLimit" should {

    "propagate the NotFound error when getSubscriptions() fails" in new Setup {
      when(applicationConnector.getSubscriptionsByApplicationId(applicationId.toString)).thenReturn(failed(NotFound()))
      intercept[NotFound] {
        await(underTest.validateSubscriptionAndRateLimit(application, ApiIdentifier("ccc", "vvv")))
      }
    }

    "throw an exception when the application has no subscriptions" in new Setup {
      mockSubscriptions(applicationConnector, successful(Seq.empty))
      intercept[InvalidSubscription] {
        await(underTest.validateSubscriptionAndRateLimit(application, ApiIdentifier("c2", "2.0")))
      }
    }

    "throw an exception when the application is not subscribed to any API with the same context of the request" in new Setup {
      mockSubscriptions(applicationConnector, successful(apis))
      intercept[InvalidSubscription] {
        await(underTest.validateSubscriptionAndRateLimit(application, ApiIdentifier("c3", "1.0")))
      }
    }

    "throw an exception when the application is not subscribed to any API with the same version of the request" in new Setup {
      mockSubscriptions(applicationConnector, successful(apis))
      intercept[InvalidSubscription] {
        await(underTest.validateSubscriptionAndRateLimit(application, ApiIdentifier("c1", "3.0")))
      }
    }

    "throw an exception when the application is not subscribed to the specific version used in the request" in new Setup {
      mockSubscriptions(applicationConnector, successful(apis))
      intercept[InvalidSubscription] {
        await(underTest.validateSubscriptionAndRateLimit(application, ApiIdentifier("c2", "1.0")))
      }
    }

    "propagate the ThrottledOut error when the rate limit is reached" in new Setup {
      val silverApplication = application.copy(rateLimitTier = SILVER)

      mockSubscriptions(applicationConnector, successful(apis))
      given(rateLimitRepository.validateAndIncrement(silverApplication.clientId, silverRateLimit)).willReturn(failed(ThrottledOut()))

      intercept[ThrottledOut] {
        await(underTest.validateSubscriptionAndRateLimit(silverApplication, subscribedApi))
      }
    }

    "return successfully when the application is subscribed and the rate limit is not reached" in new Setup {
      mockSubscriptions(applicationConnector, successful(apis))
      given(rateLimitRepository.validateAndIncrement(application.clientId, bronzeRateLimit)).willReturn(successful(()))

      await(underTest.validateSubscriptionAndRateLimit(application, subscribedApi))
    }

  }

  private def mockSubscriptions(applicationConnector: ThirdPartyApplicationConnector, eventualSubscriptions: Future[Seq[Api]]) =
    when(applicationConnector.getSubscriptionsByApplicationId(anyString())).thenReturn(eventualSubscriptions)

}
