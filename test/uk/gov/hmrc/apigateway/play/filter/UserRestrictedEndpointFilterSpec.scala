/*
 * Copyright 2016 HM Revenue & Customs
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
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.exception.GatewayError.{InvalidCredentials, InvalidScope}
import uk.gov.hmrc.apigateway.model.AuthType.USER
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.service.ScopeValidator
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

class UserRestrictedEndpointFilterSpec extends UnitSpec with MockitoSugar {

  implicit val executionContextExecutor = ExecutionContext.Implicits.global
  implicit val materializer = mock[Materializer]

  trait Setup {
    val authorityService = mock[AuthorityService]
    val scopeValidator = mock[ScopeValidator]
    val userRestrictedEndpointFilter = new UserRestrictedEndpointFilter(authorityService, scopeValidator)
  }

  "User restricted endpoint filter" should {

    val fakeRequest = FakeRequest("GET", "http://host.example/foo").withTag(X_API_GATEWAY_AUTH_TYPE, USER.toString)

    "decline a request not matching a delegated authority" in new Setup {
      mock(authorityService, InvalidCredentials())
      intercept[InvalidCredentials] {
        await(userRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      }
    }

    "decline a request not matching scopes" in new Setup {
      mock(authorityService, validAuthority())
      mock(scopeValidator, InvalidScope())
      intercept[InvalidScope] {
        await(userRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      }
    }

    "process a request which meets all requirements" in new Setup {
      mock(authorityService, validAuthority())
      mock(scopeValidator, flag = true)

      val fakeRequest = FakeRequest("GET", "http://host.example/foo").withTag(X_API_GATEWAY_AUTH_TYPE, USER.toString).withTag(X_API_GATEWAY_SCOPE, "scopeMoo")

      val result = await(userRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      result.tags.get(X_APPLICATION_CLIENT_ID) shouldBe Some("accessToken")
    }

  }

  private def mock(authorityService: AuthorityService, gatewayError: GatewayError) =
    when(authorityService.findAuthority(any[ProxyRequest])).thenThrow(gatewayError)

  private def mock(authorityService: AuthorityService, authority: Authority) =
    when(authorityService.findAuthority(any[ProxyRequest])).thenReturn(authority)

  private def mock(scopeValidationFilter: ScopeValidator, gatewayError: GatewayError) =
    when(scopeValidationFilter.validate(any(classOf[Authority]), any(classOf[Option[String]]))).thenThrow(gatewayError)

  private def mock(scopeValidationFilter: ScopeValidator, flag: Boolean) =
    when(scopeValidationFilter.validate(any(classOf[Authority]), any(classOf[Option[String]]))).thenReturn(successful(flag))

  private def validAuthority() = {
    val token = Token("accessToken", Set.empty, DateTime.now.plusMinutes(5))
    val thirdPartyDelegatedAuthority = ThirdPartyDelegatedAuthority("authBearerToken", "clientId", token)
    Authority(thirdPartyDelegatedAuthority, authExpired = false)
  }

}
