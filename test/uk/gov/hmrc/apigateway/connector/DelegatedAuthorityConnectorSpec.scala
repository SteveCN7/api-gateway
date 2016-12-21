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

package uk.gov.hmrc.apigateway.connector

import org.joda.time.DateTime
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.apigateway.connector.impl.DelegatedAuthorityConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.InvalidCredentials
import uk.gov.hmrc.apigateway.model.{Authority, ThirdPartyDelegatedAuthority, Token}
import uk.gov.hmrc.apigateway.play.binding.PlayBindings.authorityFormat
import uk.gov.hmrc.play.test.UnitSpec

class DelegatedAuthorityConnectorSpec extends UnitSpec with WsClientMocking {

    private val wsClient = mock[WSClient]
    private val delegatedAuthorityConnector = new DelegatedAuthorityConnector(wsClient, cache) {
    override lazy val serviceBaseUrl: String = "http://tpda.example"
    override lazy val expiration = 30
    override lazy val caching = false
  }

  "Delegated authority connector" should {

    "throw an exception when access token is invalid" in {
      mockWsClient(wsClient, "http://tpda.example/authority?access_token=31c99f9482de49544c6cc3374c378028", NOT_FOUND)
      intercept[InvalidCredentials] {
        await(delegatedAuthorityConnector.getByAccessToken("31c99f9482de49544c6cc3374c378028"))
      }
    }

    "return the delegated authority when access token is valid" in {
      val authority = Authority(ThirdPartyDelegatedAuthority("sandbox_token", "uKQXtOfZEmW8z5UOwHsg3ANF_fwa", Token(Set.empty, DateTime.now)))
      mockWsClient(wsClient, "http://tpda.example/authority?access_token=31c99f9482de49544c6cc3374c378028", OK, stringify(toJson(authority)))
      await(delegatedAuthorityConnector.getByAccessToken("31c99f9482de49544c6cc3374c378028")) shouldBe authority
    }

  }

}
