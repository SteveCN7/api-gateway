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
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.apigateway.config.AppContext
import uk.gov.hmrc.apigateway.connector.impl.ThirdPartyApplicationConnector
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model.RateLimitTier.{SILVER, BRONZE}
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.repository.RateLimitRepository
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future._

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val serverToken = "serverToken"
    val applicationId = UUID.randomUUID()
    val clientId = "clientId"
    val application = Application(id = applicationId, clientId = "clientId", name = "App Name", rateLimitTier = BRONZE)
    val bronzeRateLimit = 5
    val silverRateLimit = 10

    val api = ApiIdentifier("aContext", "aVersion")

    val applicationConnector = mock[ThirdPartyApplicationConnector]
    val rateLimitRepository = mock[RateLimitRepository]
    val appContext = mock[AppContext]
    val applicationService = new ApplicationService(applicationConnector, rateLimitRepository, appContext)

    given(appContext.rateLimitBronze).willReturn(bronzeRateLimit)
    given(appContext.rateLimitSilver).willReturn(silverRateLimit)
  }

  "Get application by server token" should {

    "return the application when an application exists for the given server token" in new Setup {
      when(applicationConnector.getApplicationByServerToken(serverToken)).thenReturn(successful(application))
      val result = await(applicationService.getByServerToken(serverToken))
      result shouldBe application
    }

    "propagate the error when the application cannot be fetched for the given server token" in new Setup {
      when(applicationConnector.getApplicationByServerToken(serverToken)).thenReturn(failed(NotFound()))
      intercept[NotFound] {
        await(applicationService.getByServerToken(serverToken))
      }
    }
  }

  "Get application by client id" should {

    "return the application when an application exists for the given client id" in new Setup {
      when(applicationConnector.getApplicationByClientId(clientId)).thenReturn(successful(application))
      val result = await(applicationService.getByClientId(clientId))
      result shouldBe application
    }

    "propagate the error when the application cannot be fetched for the given client id" in new Setup {
      when(applicationConnector.getApplicationByClientId(clientId)).thenReturn(failed(new RuntimeException))
      intercept[RuntimeException] {
        await(applicationService.getByClientId(clientId))
      }
    }

    "throw a 'ServerError' when the application is not found" in new Setup {
      when(applicationConnector.getApplicationByClientId(clientId)).thenReturn(failed(NotFound()))
      intercept[ServerError] {
        await(applicationService.getByClientId(clientId))
      }
    }
  }

  "validateSubscriptionAndRateLimit" should {

    "propagate the InvalidSubscription when the application is not subscribed" in new Setup {
      when(applicationConnector.validateSubscription(applicationId.toString, api)).thenReturn(failed(InvalidSubscription()))
      intercept[InvalidSubscription] {
        await(applicationService.validateSubscriptionAndRateLimit(application, api))
      }
    }

    "propagate the ThrottledOut error when the rate limit is reached" in new Setup {
      val silverApplication = application.copy(rateLimitTier = SILVER)

      mockSubscription(applicationConnector, application.id, api)
      given(rateLimitRepository.validateAndIncrement(silverApplication.clientId, silverRateLimit)).willReturn(failed(ThrottledOut()))

      intercept[ThrottledOut] {
        await(applicationService.validateSubscriptionAndRateLimit(silverApplication, api))
      }
    }

    "return successfully when the application is subscribed and the rate limit is not reached" in new Setup {
      mockSubscription(applicationConnector, application.id, api)
      given(rateLimitRepository.validateAndIncrement(application.clientId, bronzeRateLimit)).willReturn(successful(()))

      await(applicationService.validateSubscriptionAndRateLimit(application, api))
    }

  }

  private def mockSubscription(applicationConnector: ThirdPartyApplicationConnector, applicationId: UUID, api: ApiIdentifier) =
    when(applicationConnector.validateSubscription(applicationId.toString, api)).thenReturn(successful(()))

}
