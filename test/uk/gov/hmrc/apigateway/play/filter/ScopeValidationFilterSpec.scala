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

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.apigateway.exception.GatewayError.InvalidScope
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.play.test.UnitSpec

class ScopeValidationFilterSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val delegatedAuthority = mock[ThirdPartyDelegatedAuthority]
  private val token = mock[Token]
  private val authority = mock[Authority]
  private val apiDefinitionMatch = mock[ApiDefinitionMatch]

  private val scopeValidationFilter = new ScopeValidationFilter

  override def afterEach(): Unit = {
    super.afterEach()
    reset(delegatedAuthority, authority, token, apiDefinitionMatch)
  }

  "Scope Validation filter" should {

    "throw an exception when the request has no scopes" in {
      when(apiDefinitionMatch.scope).thenReturn(None)

      intercept[InvalidScope] {
        await(scopeValidationFilter.filter(authority, apiDefinitionMatch))
      }
    }

    "throw an exception when the request does not have all the required scopes" in {
      when(authority.delegatedAuthority).thenReturn(delegatedAuthority)
      when(apiDefinitionMatch.scope).thenReturn(Some("hallo hola"))
      when(delegatedAuthority.token).thenReturn(token)
      when(delegatedAuthority.token.scopes).thenReturn(Set("hola", "hallo", "nihao"))

      intercept[InvalidScope] {
        await(scopeValidationFilter.filter(authority, apiDefinitionMatch))
      }
    }

    "return true when the request has all the required scopes" in {
      when(authority.delegatedAuthority).thenReturn(delegatedAuthority)
      when(apiDefinitionMatch.scope).thenReturn(Some("ciao salut hello konnichiwa"))
      when(delegatedAuthority.token).thenReturn(token)
      when(delegatedAuthority.token.scopes).thenReturn(Set("ciao", "hello", "salut"))

      await(scopeValidationFilter.filter(authority, apiDefinitionMatch)) shouldBe true
    }

  }

}
