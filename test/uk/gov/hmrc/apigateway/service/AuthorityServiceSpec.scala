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

package uk.gov.hmrc.apigateway.service

import org.joda.time.DateTime
import org.joda.time.DateTime.now
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.apigateway.connector.impl.DelegatedAuthorityConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.{InvalidCredentials, MissingCredentials}
import uk.gov.hmrc.apigateway.model.{Authority, ProxyRequest, ThirdPartyDelegatedAuthority, Token}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.successful

class AuthorityServiceSpec extends UnitSpec with MockitoSugar {

  private val proxyRequest = mock[ProxyRequest]
  private val delegatedAuthorityConnector = mock[DelegatedAuthorityConnector]
  private val authorityService = new AuthorityService(delegatedAuthorityConnector)

  "findAuthority" should {

    "throw an exception when credentials are missing" in {
      mockProxyRequestAuthorizationHeader(None)
      intercept[MissingCredentials] {
        await(authorityService.findAuthority(proxyRequest))
      }
    }

    "throw an exception when credentials have expired" in {
      val expiredAuthority = authorityWithExpiration(now.minusMinutes(5))
      mockProxyRequestAuthorizationHeader(Some("Bearer 31c99f9482de49544c6cc3374c378028"))
      mockDelegatedAuthorityConnector(expiredAuthority)
      intercept[InvalidCredentials] {
        await(authorityService.findAuthority(proxyRequest))
      }
    }

    "return the delegated authority when credentials are valid" in {
      val unexpiredAuthority = authorityWithExpiration(now.plusMinutes(5))
      mockProxyRequestAuthorizationHeader(Some("Bearer 31c99f9482de49544c6cc3374c378028"))
      mockDelegatedAuthorityConnector(unexpiredAuthority)
      await(authorityService.findAuthority(proxyRequest)) shouldBe unexpiredAuthority
    }

  }

  private def mockProxyRequestAuthorizationHeader(maybeString: Option[String]) =
    when(proxyRequest.getHeader(AUTHORIZATION)).thenReturn(maybeString)

  private def mockDelegatedAuthorityConnector(authority: Authority) =
    when(delegatedAuthorityConnector.getByAccessToken(anyString)).thenReturn(successful(authority))

  private def authorityWithExpiration(expirationDateTime: DateTime) =
    Authority(ThirdPartyDelegatedAuthority("authBearerToken", "clientId", Token("accessToken", Set.empty, expirationDateTime)))

}
