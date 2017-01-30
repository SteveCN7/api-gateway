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

package uk.gov.hmrc.apigateway.play.filter

import akka.stream.Materializer
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{Headers, RequestHeader}
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model.AuthType.USER
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.service.{ApplicationService, AuthorityService, ScopeValidator}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.apigateway.util.RequestTags._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class UserRestrictedEndpointFilterSpec extends UnitSpec with MockitoSugar with EndpointFilterMocking {

  implicit val executionContextExecutor = ExecutionContext.Implicits.global
  implicit val materializer = mock[Materializer]

  val fakeRequest = FakeRequest("GET", "http://host.example/foo")
    .withTag(AUTH_TYPE, USER.toString)
    .withTag(API_SCOPE, "scopeMoo")
    .withTag(API_CONTEXT, "context")
    .withTag(API_VERSION, "version")
    .copy(headers = Headers("Authorization" -> "Bearer accessToken"))

  trait Setup {
    val authorityService = mock[AuthorityService]
    val applicationService = mock[ApplicationService]
    val scopeValidator = mock[ScopeValidator]
    val underTest = new UserRestrictedEndpointFilter(authorityService, applicationService, scopeValidator)

    val clientId = "clientId"
  }

  "User restricted endpoint filter" should {

    "fail without a valid access token" in new Setup {
      mockAuthority(authorityService, MissingCredentials())
      intercept[MissingCredentials] {
        await(underTest.filter(fakeRequest, ProxyRequest(fakeRequest)))
      }
    }

    "not add additional tags in the request header if the auth-type is not `USER` " in new Setup {
      val nonUserRequest = fakeRequest.withTag(AUTH_TYPE, generateRandomAuthType(USER))

      val result = await(underTest.filter(nonUserRequest, ProxyRequest(nonUserRequest)))
      result.tags shouldBe nonUserRequest.tags
    }

    "decline a request not matching a delegated authority" in new Setup {
      mockAuthority(authorityService, InvalidCredentials())
      intercept[InvalidCredentials] {
        await(underTest.filter(fakeRequest, ProxyRequest(fakeRequest)))
      }
    }

    "decline a request with a valid server token" in new Setup {
      val serverToken = "serverToken"
      val request = fakeRequest.copy(headers = Headers(AUTHORIZATION -> serverToken))

      mockAuthority(authorityService, NotFound())
      mockApplicationByServerToken(applicationService, serverToken, anApplication())
      intercept[IncorrectAccessTokenType] {
        await(underTest.filter(request, ProxyRequest(request)))
      }
    }

    "propagate the error, when there is a failure in fetching the application" in new Setup {
      mockAuthority(authorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, ServerError())
      intercept[ServerError] {
        await(underTest.filter(fakeRequest, ProxyRequest(fakeRequest)))
      }
    }

    "decline a request not matching the application API subscriptions" in new Setup {
      mockAuthority(authorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, anApplication())
      mockApiSubscriptions(applicationService, InvalidSubscription())
      intercept[InvalidSubscription] {
        await(underTest.filter(fakeRequest, ProxyRequest(fakeRequest)))
      }
    }

    "decline a request not matching scopes" in new Setup {
      mockAuthority(authorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, anApplication())
      mockApiSubscriptions(applicationService)
      mockScopeValidation(scopeValidator, InvalidScope())
      intercept[InvalidScope] {
        await(underTest.filter(fakeRequest, ProxyRequest(fakeRequest)))
      }
    }

    "process a request which meets all requirements" in new Setup {
      mockAuthority(authorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, anApplication())
      mockApiSubscriptions(applicationService)

      val result: Future[RequestHeader] = await(underTest.filter(fakeRequest, ProxyRequest(fakeRequest)))

      result.tags(AUTH_AUTHORIZATION) shouldBe "Bearer authBearerToken"
      result.tags(CLIENT_ID) shouldBe clientId
    }

  }

}
