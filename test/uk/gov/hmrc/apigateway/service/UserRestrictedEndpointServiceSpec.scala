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
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsJson, Headers, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.model.{ApiIdentifier, ApiRequest, ProxyRequest}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future._

class UserRestrictedEndpointServiceSpec extends UnitSpec with MockitoSugar with RoutingServicesMocks {

  private val fakeRequest = FakeRequest(
    method = "GET",
    uri = "http://host.example/foo",
    headers = Headers(AUTHORIZATION -> "Bearer accessToken"),
    body = AnyContentAsJson(Json.parse("""{}""")))

  private val apiRequest = ApiRequest(
    apiIdentifier = ApiIdentifier("context", "version"),
    authType = USER,
    apiEndpoint = "http://host.example/foo/context",
    scope = Some("scopeMoo"))

  private trait Setup {
    val authorityService = mock[AuthorityService]
    val applicationService = mock[ApplicationService]
    val scopeValidator = mock[ScopeValidator]

    val userRestrictedEndpointService = new UserRestrictedEndpointService(authorityService, applicationService, scopeValidator)

    val clientId = "clientId"
    val application = anApplication()
  }

  "routeRequest" should {

    "fail without a valid access token" in new Setup {
      mockAuthority(authorityService, MissingCredentials(mock[Request[AnyContent]], mock[ApiRequest]))

      intercept[MissingCredentials] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "decline a request not matching a delegated authority" in new Setup {
      mockAuthority(authorityService, InvalidCredentials(mock[Request[AnyContent]], mock[ApiRequest]))

      intercept[InvalidCredentials] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "decline a request with a valid server token" in new Setup {
      val serverToken = "serverToken"
      val request = fakeRequest.withHeaders(AUTHORIZATION -> serverToken)

      mockAuthority(authorityService, NotFound())
      mockApplicationByServerToken(applicationService, serverToken, application)

      intercept[IncorrectAccessTokenType] {
        await(userRestrictedEndpointService.routeRequest(request, ProxyRequest(request), apiRequest))
      }
    }

    "propagate the error, when there is a failure in fetching the application" in new Setup {
      mockAuthority(authorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, ServerError())

      intercept[ServerError] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "decline a request not matching the application API subscriptions" in new Setup {
      mockAuthority(authorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, failed(InvalidSubscription()))

      intercept[InvalidSubscription] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "decline a request not matching scopes" in new Setup {
      mockAuthority(authorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, successful(()))
      mockScopeValidation(scopeValidator, InvalidScope())

      intercept[InvalidScope] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "propagate the error, when the application has reached its rate limit" in new Setup {
      mockAuthority(authorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, failed(ThrottledOut()))

      intercept[ThrottledOut] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "route a request which meets all requirements" in new Setup {
      mockAuthority(authorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, successful(()))

      val expectedResult = apiRequest.copy(
        userOid = Some("userOID"),
        clientId = Some("clientId"),
        bearerToken = Some("Bearer authBearerToken"))

      val result = await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))

      result shouldBe expectedResult
    }

  }

}
