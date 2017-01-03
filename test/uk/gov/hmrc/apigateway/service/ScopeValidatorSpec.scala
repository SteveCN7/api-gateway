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

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.apigateway.exception.GatewayError.InvalidScope
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.play.test.UnitSpec

class ScopeValidatorSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val delegatedAuthority = mock[ThirdPartyDelegatedAuthority]
    val token = mock[Token]
    val authority = mock[Authority]
    val apiDefinitionMatch = mock[ApiDefinitionMatch]
    val scopeValidator = new ScopeValidator

    when(authority.delegatedAuthority).thenReturn(delegatedAuthority)
    when(delegatedAuthority.token).thenReturn(token)
    when(delegatedAuthority.token.scopes).thenReturn(Set("read:scope", "write:scope", "read:another-scope"))
  }

  "Scope validator" should {

    "throw an exception when the request has no scopes" in new Setup {
      intercept[InvalidScope] {
        await(scopeValidator.validate(authority, None))
      }
    }

    "throw an exception when the request scope is empty" in new Setup {
      intercept[InvalidScope] {
        await(scopeValidator.validate(authority, Some("")))
      }
    }

    "throw an exception when the request contains multiple scopes" in new Setup {
      intercept[InvalidScope] {
        await(scopeValidator.validate(authority, Some("read:scope write:scope")))
      }
    }

    "throw an exception when the request does not have any of the required scopes" in new Setup {
      intercept[InvalidScope] {
        await(scopeValidator.validate(authority, Some("read:scope-1")))
      }
    }

    "return true when the request has all the required scopes" in new Setup {
      await(scopeValidator.validate(authority, Some("read:scope"))) shouldBe true
    }

  }

}
