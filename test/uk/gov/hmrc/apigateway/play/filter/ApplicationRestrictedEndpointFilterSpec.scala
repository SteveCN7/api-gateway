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
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.service.{ApplicationService, AuthorityService}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.failed

class ApplicationRestrictedEndpointFilterSpec extends UnitSpec with MockitoSugar {

  implicit val executionContextExecutor = ExecutionContext.Implicits.global
  implicit val materializer = mock[Materializer]
  val serverToken = "serverToken"

  trait Setup {
    val mockAuthorityService = mock[AuthorityService]
    val mockApplicationService = mock[ApplicationService]
    val applicationRestrictedEndpointFilter = new ApplicationRestrictedEndpointFilter(mockAuthorityService, mockApplicationService)
  }

  "Application restricted endpoint filter" should {

    val fakeRequest = FakeRequest("GET", "http://host.example/foo")
      .withHeaders((AUTHORIZATION -> s"Bearer $serverToken"))
      .withTag(X_API_GATEWAY_AUTH_TYPE, APPLICATION.toString)

    "process a request with a valid user token" in new Setup {
      val application = anApplication()
      mock(mockAuthorityService, validAuthority())
      mockByClientId(mockApplicationService, validAuthority(), application)
      val result = await(applicationRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      result.tags.get(X_APPLICATION_ID) shouldBe Some(application.id.toString)
    }

    "process a request with a valid server token" in new Setup {
      val application = anApplication()
      mock(mockAuthorityService, NotFound())
      mockByServerToken(mockApplicationService, serverToken, application)
      val result = await(applicationRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      result.tags.get(X_APPLICATION_ID) shouldBe Some(application.id.toString)
    }

   "propagate an invalid credentials error for an expired user token" in new Setup {
      mock(mockAuthorityService, InvalidCredentials())
      intercept[InvalidCredentials] {
        await(applicationRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      }
    }

    "propagate an invalid credentials error for an invalid server token" in new Setup {
      val application = anApplication()
      mock(mockAuthorityService, NotFound())
      mockByServerToken(mockApplicationService, serverToken, InvalidCredentials())

      intercept[InvalidCredentials] {
        await(applicationRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      }
    }

    "return a missing credentials error for a request without an authorization header" in new Setup {
      val fakeRequest = FakeRequest("GET", "http://host.example/foo")
        .withTag(X_API_GATEWAY_AUTH_TYPE, APPLICATION.toString)

      intercept[MissingCredentials] {
        await(applicationRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      }
    }

    "ignore a request for a resource which is not application restricted" in new Setup {
      val fakeRequest = FakeRequest("GET", "http://host.example/foo")
        .withTag(X_API_GATEWAY_AUTH_TYPE, USER.toString)

      val result = await(applicationRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      result.tags shouldBe fakeRequest.tags
    }
  }

  private def mock(authorityService: AuthorityService, gatewayError: GatewayError) =
    when(authorityService.findAuthority(any[ProxyRequest])).thenReturn(failed(gatewayError))

  private def mock(authorityService: AuthorityService, authority: Authority) =
    when(authorityService.findAuthority(any[ProxyRequest])).thenReturn(authority)

  private def mockByClientId(applicationService: ApplicationService, authority: Authority, application: Application) =
    when(applicationService.getByClientId(authority.delegatedAuthority.clientId)).thenReturn(application)

  private def mockByServerToken(applicationService: ApplicationService, accessToken: String, application: Application) =
    when(applicationService.getByServerToken(accessToken)).thenReturn(application)

  private def mockByServerToken(applicationService: ApplicationService, accessToken: String, gatewayError: GatewayError) =
    when(applicationService.getByServerToken(accessToken)).thenReturn(failed(gatewayError))

  private def validAuthority() = {
    val token = Token(serverToken, Set.empty, DateTime.now.plusMinutes(5))
    val thirdPartyDelegatedAuthority = ThirdPartyDelegatedAuthority("authBearerToken", "clientId", token)
    Authority(thirdPartyDelegatedAuthority, authExpired = false)
  }

  private def anApplication() = Application(UUID.randomUUID(), "App Name")
}
