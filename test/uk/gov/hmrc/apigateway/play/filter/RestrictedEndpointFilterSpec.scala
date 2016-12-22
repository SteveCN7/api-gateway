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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.{FORBIDDEN, OK, UNAUTHORIZED}
import play.api.mvc.Results.Ok
import play.api.mvc.{Headers, RequestHeader, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.exception.GatewayError.{InvalidCredentials, InvalidScope}
import uk.gov.hmrc.apigateway.model.{Authority, ProxyRequest, ThirdPartyDelegatedAuthority, Token}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class RestrictedEndpointFilterSpec extends UnitSpec with MockitoSugar {

  implicit val executionContextExecutor = ExecutionContext.Implicits.global
  implicit val materializer = mock[Materializer]

  trait Setup {
    val delegatedAuthorityFilter = mock[DelegatedAuthorityFilter]
    val scopeValidationFilter = mock[ScopeValidationFilter]
    val restrictedEndpointFilter = new RestrictedEndpointFilter(delegatedAuthorityFilter, scopeValidationFilter)
  }

  "Restricted endpoint filter" should {

    val fakeRequest = FakeRequest("GET", "http://host.example/foo", Headers("" -> ""), """{"request":"json"}""")
    val nextFilter: (RequestHeader) => Future[Result] = { requestHeader => successful(Ok("""{"response":"json"}""")) }

    "decline a request not matching a delegated authority" in new Setup {
      pending
      mock(delegatedAuthorityFilter, InvalidCredentials())
      status(restrictedEndpointFilter.apply(nextFilter)(fakeRequest)) shouldBe UNAUTHORIZED
    }

    "decline a request not matching scopes" in new Setup {
      mock(delegatedAuthorityFilter, Set("valid:scope"))
      mock(scopeValidationFilter, InvalidScope())
      status(restrictedEndpointFilter.apply(nextFilter)(fakeRequest.withTag(X_API_GATEWAY_AUTH_TYPE, "USER"))) shouldBe FORBIDDEN
    }

    "process a request which meets all requirements" in new Setup {
      mock(delegatedAuthorityFilter, Set("valid:scope"))
      mock(scopeValidationFilter, boolean = true)
      status(restrictedEndpointFilter.apply(nextFilter)(fakeRequest)) shouldBe OK
    }

  }

  private def mock(delegatedAuthorityFilter: DelegatedAuthorityFilter, gatewayError: GatewayError) =
    when(delegatedAuthorityFilter.filter(any[ProxyRequest])).thenThrow(gatewayError)

  private def mock(scopeValidationFilter: ScopeValidationFilter, gatewayError: GatewayError) =
    when(scopeValidationFilter.filter(any(classOf[Authority]), any(classOf[Option[String]]))).thenThrow(gatewayError)

  private def mock(scopeValidationFilter: ScopeValidationFilter, boolean: Boolean) =
    when(scopeValidationFilter.filter(any(classOf[Authority]), any(classOf[Option[String]]))).thenReturn(successful(boolean))

  private def mock(delegatedAuthorityFilter: DelegatedAuthorityFilter, scopes: Set[String]) = {
    val delegatedAuthority = mock[ThirdPartyDelegatedAuthority]
    val authority = mock[Authority]

    when(authority.delegatedAuthority).thenReturn(delegatedAuthority)
    when(delegatedAuthority.token).thenReturn(mock[Token])
    when(delegatedAuthority.token.scopes).thenReturn(scopes)
    when(delegatedAuthorityFilter.filter(any[ProxyRequest])).thenReturn(authority)
  }

}
