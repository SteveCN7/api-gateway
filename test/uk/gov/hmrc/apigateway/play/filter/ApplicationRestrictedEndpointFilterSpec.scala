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
import uk.gov.hmrc.apigateway.exception.GatewayError.{InvalidCredentials, MissingCredentials}
import uk.gov.hmrc.apigateway.model.AuthType.APPLICATION
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext

class ApplicationRestrictedEndpointFilterSpec extends UnitSpec with MockitoSugar {

  implicit val executionContextExecutor = ExecutionContext.Implicits.global
  implicit val materializer = mock[Materializer]

  trait Setup {
    val delegatedAuthorityFilter = mock[DelegatedAuthorityFilter]
    val applicationRestrictedEndpointFilter = new ApplicationRestrictedEndpointFilter(delegatedAuthorityFilter)
  }

  "Application restricted endpoint filter" should {

    val fakeRequest = FakeRequest("GET", "http://host.example/foo").withTag(X_API_GATEWAY_AUTH_TYPE, APPLICATION.toString)

    "process a request with a valid access token" in new Setup {
      mock(delegatedAuthorityFilter, validAuthority())
      val result = await(applicationRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      result.tags.get(X_APPLICATION_CLIENT_ID) shouldBe Some("clientId")
    }

    "attempt to recover from a request without a valid access token" in new Setup {
      mock(delegatedAuthorityFilter, InvalidCredentials())
      intercept[MissingCredentials] {
        await(applicationRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      }
    }

    "process a request with a valid server token" in new Setup {
      mock(delegatedAuthorityFilter, InvalidCredentials())
      val fakeRequest = FakeRequest("GET", "http://host.example/foo").withTag(X_API_GATEWAY_AUTH_TYPE, APPLICATION.toString).withTag(AUTHORIZATION, "Bearer serverToken")
      val result = await(applicationRestrictedEndpointFilter.filter(fakeRequest, ProxyRequest(fakeRequest)))
      result.tags.get(X_APPLICATION_CLIENT_ID) shouldBe Some("serverToken")
    }

  }

  private def mock(delegatedAuthorityFilter: DelegatedAuthorityFilter, gatewayError: GatewayError) =
    when(delegatedAuthorityFilter.filter(any[ProxyRequest])).thenThrow(gatewayError)

  private def mock(delegatedAuthorityFilter: DelegatedAuthorityFilter, authority: Authority) =
    when(delegatedAuthorityFilter.filter(any[ProxyRequest])).thenReturn(authority)

  private def validAuthority() = {
    val token = Token("accessToken", Set.empty, DateTime.now.plusMinutes(5))
    val thirdPartyDelegatedAuthority = ThirdPartyDelegatedAuthority("authBearerToken", "clientId", token)
    Authority(thirdPartyDelegatedAuthority, authExpired = false)
  }

}
