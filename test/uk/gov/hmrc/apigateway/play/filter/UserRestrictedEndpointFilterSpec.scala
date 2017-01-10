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

import java.util.UUID

import akka.stream.Materializer
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.exception.GatewayError.{IncorrectAccessTokenType, InvalidCredentials, InvalidScope, NotFound}
import uk.gov.hmrc.apigateway.model.AuthType.USER
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.service.{ApplicationService, AuthorityService, ScopeValidator}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class UserRestrictedEndpointFilterSpec extends UnitSpec with MockitoSugar {

  implicit val executionContextExecutor = ExecutionContext.Implicits.global
  implicit val materializer = mock[Materializer]

  val accessToken = "accessToken"
  val fakeRequest = FakeRequest("GET", "http://host.example/foo")
    .withHeaders((AUTHORIZATION -> s"Bearer $accessToken"))
    .withTag(X_API_GATEWAY_AUTH_TYPE, USER.toString)
  val proxyRequest = ProxyRequest(fakeRequest)

  trait Setup {
    val authorityService = mock[AuthorityService]
    val applicationService = mock[ApplicationService]
    val scopeValidator = mock[ScopeValidator]
    val userRestrictedEndpointFilter = new UserRestrictedEndpointFilter(authorityService, applicationService, scopeValidator)
  }

  "User restricted endpoint filter" should {

    "decline a request not matching a delegated authority" in new Setup {
      mock(authorityService, NotFound())
      mock(applicationService, InvalidCredentials())
      intercept[InvalidCredentials] {
        await(userRestrictedEndpointFilter.filter(fakeRequest, proxyRequest))
      }
    }

    "decline a request not matching scopes" in new Setup {
      mock(authorityService, validAuthority())
      mock(scopeValidator, InvalidScope())
      intercept[InvalidScope] {
        await(userRestrictedEndpointFilter.filter(fakeRequest, proxyRequest))
      }
    }

    "decline a request when attempting to use a valid serverToken" in new Setup {
      mock(authorityService, NotFound())
      mock(applicationService, anApplication())
      intercept[IncorrectAccessTokenType] {
        await(userRestrictedEndpointFilter.filter(fakeRequest, proxyRequest))
      }
    }

    "process a request with valid authority and for the correct scopes" in new Setup {
      mock(authorityService, validAuthority())
      mock(scopeValidator, flag = true)

      val fakeRequest = FakeRequest("GET", "http://host.example/foo").withTag(X_API_GATEWAY_AUTH_TYPE, USER.toString).withTag(X_API_GATEWAY_SCOPE, "scopeMoo")

      val result = await(userRestrictedEndpointFilter.filter(fakeRequest, proxyRequest))

      result.tags.get(X_APPLICATION_ID) shouldBe Some("clientId")
      result.tags.get(AUTHORIZATION) shouldBe Some("Bearer authBearerToken")
    }
  }

  private def mock(authorityService: AuthorityService, gatewayError: GatewayError) =
    when(authorityService.findAuthority(proxyRequest)).thenReturn(Future.failed(gatewayError))

  private def mock(authorityService: AuthorityService, authority: Authority) =
    when(authorityService.findAuthority(proxyRequest)).thenReturn(authority)

  private def mock(applicationService: ApplicationService, gatewayError: GatewayError) =
    when(applicationService.getByServerToken(accessToken)).thenThrow(gatewayError)

  private def mock(applicationService: ApplicationService, application: Application) =
    when(applicationService.getByServerToken(accessToken)).thenReturn(application)

  private def mock(scopeValidationFilter: ScopeValidator, gatewayError: GatewayError) =
    when(scopeValidationFilter.validate(any(classOf[ThirdPartyDelegatedAuthority]), any(classOf[Option[String]]))).thenThrow(gatewayError)

  private def mock(scopeValidationFilter: ScopeValidator, flag: Boolean) =
    when(scopeValidationFilter.validate(any(classOf[ThirdPartyDelegatedAuthority]), any(classOf[Option[String]]))).thenReturn(successful(flag))

  private def validAuthority() = {
    val token = Token(accessToken, Set.empty, DateTime.now.plusMinutes(5))
    val thirdPartyDelegatedAuthority = ThirdPartyDelegatedAuthority("authBearerToken", "clientId", token)
    Authority(thirdPartyDelegatedAuthority, authExpired = false)
  }

  private def anApplication() = {
    Application(UUID.randomUUID(), "App Name")
  }
}
