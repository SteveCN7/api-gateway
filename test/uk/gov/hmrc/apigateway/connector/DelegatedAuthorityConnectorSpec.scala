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
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import uk.gov.hmrc.apigateway.connector.impl.DelegatedAuthorityConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.InvalidCredentials
import uk.gov.hmrc.apigateway.model.{Authority, ThirdPartyDelegatedAuthority, Token}
import uk.gov.hmrc.apigateway.play.binding.PlayBindings.authorityFormat
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.successful

class DelegatedAuthorityConnectorSpec extends UnitSpec with MockitoSugar {

  private val wsClient = mock[WSClient]
  private val delegatedAuthorityConnector = new DelegatedAuthorityConnector(wsClient) {
    override def baseUrl(serviceName: String): String = "http://tpda.example"
  }

  "Delegated authority connector" should {

    "throw an exception when access token is invalid" in {
      mockWsClientToReturn(NOT_FOUND)
      intercept[InvalidCredentials] {
        await(delegatedAuthorityConnector.getByAccessToken("31c99f9482de49544c6cc3374c378028"))
      }
    }

    "return the delegated authority when access token is valid" in {
      val authority = Authority(ThirdPartyDelegatedAuthority("sandbox_token", "uKQXtOfZEmW8z5UOwHsg3ANF_fwa", Token(Set.empty, DateTime.now)))
      mockWsClientToReturn(OK, stringify(toJson(authority)))
      await(delegatedAuthorityConnector.getByAccessToken("31c99f9482de49544c6cc3374c378028")) shouldBe authority
    }

  }

  // TODO this will need to be reused between tests, extract to class?
  private def mockWsClientToReturn(httpResponseCode: Int, responseJson: String = "{}") = {
    val wsRequest = mock[WSRequest]
    when(wsClient.url(anyString)).thenReturn(wsRequest)
    val wsResponse = mock[WSResponse]
    when(wsResponse.json).thenReturn(parse(responseJson))
    when(wsRequest.get()).thenReturn(successful(wsResponse))
    when(wsResponse.status).thenReturn(httpResponseCode)
  }

}
