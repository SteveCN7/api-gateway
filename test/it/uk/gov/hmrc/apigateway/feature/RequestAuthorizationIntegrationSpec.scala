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

package it.uk.gov.hmrc.apigateway.feature

import it.uk.gov.hmrc.apigateway.BaseFeatureSpec
import org.joda.time.DateTime.now
import play.api.http.Status._
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.util.HttpHeaders.{ACCEPT, AUTHORIZATION}

import scalaj.http.Http

class RequestAuthorizationIntegrationSpec extends BaseFeatureSpec {

  val anApiDefinition = ApiDefinition("api-simulator", api.url,
    Seq(
      ApiVersion("1.0", Seq(
        ApiEndpoint("userScope1", "GET", AuthType.USER, scope = Some("scope1")),
        ApiEndpoint("userScope2", "GET", AuthType.USER, scope = Some("scope2")),
        ApiEndpoint("open", "GET", AuthType.NONE))
      ))
    )
  val apiResponse = """{"response": "ok"}"""
  val accessToken = "accessToken"
  val authority = Authority(
    ThirdPartyDelegatedAuthority("authBearerToken", "clientId", Token(accessToken, Set("scope1"), now().plusHours(3))),
    authExpired = false)

  override def beforeEach() {
    super.beforeEach()

    Given("An API Definition exists")
    apiDefinition.willReturnTheApiDefinition(anApiDefinition)

    And("The API returns a response")
    api.willReturnTheResponse(apiResponse)
  }

  feature("User Restricted endpoint") {

    scenario("A restricted request without an 'authorization' http header is not proxied") {
      Given("A request without an 'authorization' http header")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope1").header(ACCEPT, "application/vnd.hmrc.1.0+json")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '401' unauthorized")
      assertCodeIs(httpResponse, UNAUTHORIZED)

      And("The response message code is 'MISSING_CREDENTIALS'")
      assertBodyIs(httpResponse, """ {"code":"MISSING_CREDENTIALS","message":"Authentication information is not provided"} """)
    }

    scenario("A restricted request with an invalid 'authorization' http header is not proxied") {

      Given("A request")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope1")
        .header(ACCEPT, "application/vnd.hmrc.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("An authority does not exist for the access token")
      thirdPartyDelegatedAuthority.willNotReturnAnAuthorityForAccessToken(accessToken)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '401' unauthorized")
      assertCodeIs(httpResponse, UNAUTHORIZED)

      And("The response message code is 'INVALID_CREDENTIALS'")
      assertBodyIs(httpResponse, """ {"code":"INVALID_CREDENTIALS","message":"Invalid Authentication information provided"} """)
    }

    scenario("A restricted request with invalid scopes is not proxied") {
      Given("A request to an endpoint requiring 'scope2'")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope2")
        .header(ACCEPT, "application/vnd.hmrc.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The access token matches an authority with 'scope1'")
      thirdPartyDelegatedAuthority.willReturnTheAuthorityForAccessToken(accessToken, authority)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '403' forbidden")
      assertCodeIs(httpResponse, FORBIDDEN)

      And("The response message code is 'INVALID_SCOPE'")
      assertBodyIs(httpResponse, """ {"code":"INVALID_SCOPE","message":"Cannot access the required resource. Ensure this token has all the required scopes."} """)
    }

    scenario("A request passing checks for a user restricted endpoint is proxied") {
      Given("A request to an endpoint requiring 'scope1'")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope1")
        .header(ACCEPT, "application/vnd.hmrc.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The access token matches an authority with 'scope1'")
      thirdPartyDelegatedAuthority.willReturnTheAuthorityForAccessToken(accessToken, authority)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The request is proxied to the microservice")
      assertCodeIs(httpResponse, OK)
      assertBodyIs(httpResponse, apiResponse)
    }
  }

  feature("Open endpoint") {

    scenario("A request passing checks for an open endpoint is proxied") {

      Given("a request which passes checks for an open endpoint")
      val httpRequest = Http(s"$serviceUrl/api-simulator/open")
        .header(ACCEPT, "application/vnd.hmrc.1.0+json")

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The request is proxied to the microservice")
      assertCodeIs(httpResponse, OK)
      assertBodyIs(httpResponse, apiResponse)
    }
  }
}
