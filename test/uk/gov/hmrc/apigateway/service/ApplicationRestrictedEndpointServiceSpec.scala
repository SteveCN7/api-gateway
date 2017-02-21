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

import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Headers
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.model.{ApiIdentifier, ApiRequest, ProxyRequest}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.{failed, successful}

class ApplicationRestrictedEndpointServiceSpec extends UnitSpec with MockitoSugar with RoutingServicesMocks {

  private trait Setup {

    val serverToken = "accessToken"
    val clientId = "clientId"
    val application = anApplication()

    val apiRequest = ApiRequest(
      timeInNanos = Some(10000),
      apiIdentifier = ApiIdentifier("context", "version"),
      authType = APPLICATION,
      apiEndpoint = "http://host.example/foo/context")

    val basicRequest = new FakeRequest(
      method = "GET",
      uri = "http://host.example/foo",
      headers = Headers(),
      body = "")
    val applicationRequestWithToken = basicRequest.copy(headers = Headers(AUTHORIZATION -> s"Bearer $serverToken"))

    val authorityService = mock[AuthorityService]
    val applicationService = mock[ApplicationService]

    val applicationRestrictedEndpointService = new ApplicationRestrictedEndpointService(authorityService, applicationService)
  }

  "routeRequest" should {

    "fail with a request not matching authority" in new Setup {
      intercept[MissingCredentials] {
        await(applicationRestrictedEndpointService.routeRequest(ProxyRequest(basicRequest), apiRequest))
      }
    }

    "propagate the error, when there is a failure in fetching the application by server token" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, ServerError())

      intercept[ServerError] {
        await(applicationRestrictedEndpointService.routeRequest(ProxyRequest(applicationRequestWithToken), apiRequest))
      }
    }

    "fail, with a request without a valid access token" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, NotFound())
      mockAuthority(authorityService, NotFound())

      intercept[InvalidCredentials] {
        await(applicationRestrictedEndpointService.routeRequest(ProxyRequest(applicationRequestWithToken), apiRequest))
      }
    }

    "propagate the error, when there is a failure in fetching the application by client id" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, NotFound())
      mockAuthority(authorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, ServerError())

      intercept[ServerError] {
        await(applicationRestrictedEndpointService.routeRequest(ProxyRequest(applicationRequestWithToken), apiRequest))
      }
    }

    "propagate the error, when there is a failure in finding the application subscriptions" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, NotFound())
      mockAuthority(authorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, failed(ServerError()))

      intercept[ServerError] {
        await(applicationRestrictedEndpointService.routeRequest(ProxyRequest(applicationRequestWithToken), apiRequest))
      }
    }

    "fail with ThrottledOut when the application rate limit has been reached" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, NotFound())
      mockAuthority(authorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, failed(ThrottledOut()))

      intercept[ThrottledOut] {
        await(applicationRestrictedEndpointService.routeRequest(ProxyRequest(applicationRequestWithToken), apiRequest))
      }
    }

    "route a request with a valid access token that meets all requirements" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, NotFound())
      mockAuthority(authorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, successful(()))

      val expectedResult = apiRequest.copy(clientId = Some(clientId))
      val result = await(applicationRestrictedEndpointService.routeRequest(ProxyRequest(applicationRequestWithToken), apiRequest))

      result shouldBe expectedResult
    }

    "route a request with a valid server token that meets all requirements" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, successful(()))

      val expectedResult = apiRequest.copy(clientId = Some(clientId))
      val result = await(applicationRestrictedEndpointService.routeRequest(ProxyRequest(applicationRequestWithToken), apiRequest))

      result shouldBe expectedResult
    }

  }

}
