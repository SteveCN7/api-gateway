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

import it.uk.gov.hmrc.apigateway.BaseIntegrationSpec
import org.joda.time.DateTime
import play.api.http.Status._
import play.api.libs.json.Json.{parse, stringify, toJson}
import uk.gov.hmrc.apigateway.exception.GatewayError.NotFound
import uk.gov.hmrc.apigateway.model.{Authority, ThirdPartyDelegatedAuthority, Token}
import uk.gov.hmrc.apigateway.play.binding.PlayBindings.authorityFormat
import uk.gov.hmrc.apigateway.util.HttpHeaders.{ACCEPT, AUTHORIZATION}

import scalaj.http.Http

class RequestProxyingIntegrationSpec extends BaseIntegrationSpec {

  feature("The API gateway proxies requests to downstream services") {

    info("As a third party software developer")
    info("I want to be able to make requests via the API gateway")
    info("So that I can invoke services on the API platform")

    scenario("a request without an 'accept' http header is not proxied") {
      Given("a request without an 'accept' http header")
      val httpRequest = Http(s"$apiGatewayUrl/foo")

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the http response is '400' bad request")
      assertCodeIs(httpResponse, BAD_REQUEST)

      And("the response message code is 'ACCEPT_HEADER_INVALID'")
      assertBodyIs(httpResponse, """ {"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"} """)
    }

    scenario("a request with a malformed 'accept' http header is not proxied") {
      Given("a request with a malformed 'accept' http header")
      val httpRequest = Http(s"$apiGatewayUrl/foo").header(ACCEPT, "application/json")

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the response is http '400' bad request")
      assertCodeIs(httpResponse, BAD_REQUEST)

      And("the response message code is 'ACCEPT_HEADER_INVALID'")
      assertBodyIs(httpResponse, """ {"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"} """)
    }

    scenario("a request whose context cannot be matched is not proxied") {
      Given("a request for a non existent context")
      val httpRequest = Http(s"$apiGatewayUrl/foo").header(ACCEPT, "application/vnd.hmrc.1.0+json")
      mockWsClient(wsClient, "http://ad.example:9001/api-definition?context=foo", NOT_FOUND)

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the http response is '404' not found")
      assertCodeIs(httpResponse, NOT_FOUND)

      And("the response message code is 'NOT_FOUND'")
      assertBodyIs(httpResponse, """ {"code":"NOT_FOUND","message":"Requested resource could not be found"} """)
    }

    scenario("a request whose resource cannot be matched is not proxied") {
      Given("a request for a non existent resource")
      val httpRequest = Http(s"$apiGatewayUrl/api-simulator/non-existent-resource").header(ACCEPT, "application/vnd.hmrc.1.0+json")
      mockWsClient(wsClient, "http://ad.example:9001/api-definition?context=api-simulator", OK, loadStubbedJson("api-definition/api-simulator"))

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the http response is '404' not found")
      assertCodeIs(httpResponse, NOT_FOUND)

      And("the response message code is 'MATCHING_RESOURCE_NOT_FOUND'")
      assertBodyIs(httpResponse,
        """{
          "code":"MATCHING_RESOURCE_NOT_FOUND",
          "message":"A resource with the name in the request cannot be found in the API"
          } """)
    }

    scenario("a request whose version cannot be matched is not proxied") {
      Given("a request without a non existent version")
      val httpRequest = Http(s"$apiGatewayUrl/api-simulator/version-2-0-endpoint").header(ACCEPT, "application/vnd.hmrc.1.0+json")
      mockWsClient(wsClient, "http://ad.example:9001/api-definition?context=api-simulator", OK, loadStubbedJson("api-definition/api-simulator"))

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the http response is '404' not found")
      assertCodeIs(httpResponse, NOT_FOUND)

      And("the response message code is 'MATCHING_RESOURCE_NOT_FOUND'")
      assertBodyIs(httpResponse,
        """{
          "code":"MATCHING_RESOURCE_NOT_FOUND",
          "message":"A resource with the name in the request cannot be found in the API"
          } """)
    }

    scenario("a request without an 'authorization' http header is not proxied") {
      Given("a request without an 'authorization' http header")
      val httpRequest = Http(s"$apiGatewayUrl/api-simulator/user/latency/1").header(ACCEPT, "application/vnd.hmrc.1.0+json")
      mockWsClient(wsClient, "http://ad.example:9001/api-definition?context=api-simulator", OK, loadStubbedJson("api-definition/api-simulator"))

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the http response is '401' unauthorized")
      assertCodeIs(httpResponse, UNAUTHORIZED)

      And("the response message code is 'MISSING_CREDENTIALS'")
      assertBodyIs(httpResponse, """ {"code":"MISSING_CREDENTIALS","message":"Authentication information is not provided"} """)
    }

    scenario("a request with an invalid 'authorization' http header is not proxied") {
      Given("a request with an invalid 'authorization' http header")
      val httpRequest = Http(s"$apiGatewayUrl/api-simulator/user/latency/1").header(ACCEPT, "application/vnd.hmrc.1.0+json").header(AUTHORIZATION, "80d964331707baf8872179c805351")
      mockWsClient(wsClient, "http://ad.example:9001/api-definition?context=api-simulator", OK, loadStubbedJson("api-definition/api-simulator"))
      mockWsClient(wsClient, "http://tpda.example:9002/authority?access_token=80d964331707baf8872179c805351", OK, loadStubbedJson("authority/80d964331707baf8872179c805351"))

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the http response is '401' unauthorized")
      assertCodeIs(httpResponse, UNAUTHORIZED)

      And("the response message code is 'INVALID_CREDENTIALS'")
      assertBodyIs(httpResponse, """ {"code":"INVALID_CREDENTIALS","message":"Invalid Authentication information provided"} """)
    }

    scenario("a request with invalid scopes is not proxied") {
      Given("a request with invalid scopes")
      val httpRequest = Http(s"$apiGatewayUrl/api-simulator/user-restricted-version-2-0-endpoint")
        .header(ACCEPT, "application/vnd.hmrc.2.0+json")
        .header(AUTHORIZATION, "Bearer 80d964331707baf8872179c805353")
      mockWsClient(wsClient, "http://ad.example:9001/api-definition?context=api-simulator", OK, loadStubbedJson("api-definition/api-simulator"))
      mockWsClient(wsClient, "http://tpda.example:9002/authority?access_token=80d964331707baf8872179c805353", OK, loadStubbedDelegatedAuthority("80d964331707baf8872179c805353"))

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the http response is '403' forbidden")
      assertCodeIs(httpResponse, FORBIDDEN)

      And("the response message code is 'INVALID_SCOPE'")
      assertBodyIs(httpResponse, """ {"code":"INVALID_SCOPE","message":"Cannot access the required resource. Ensure this token has all the required scopes."} """)
    }

    scenario("a request passing checks for a user restricted endpoint is proxied") {
      Given("a request which passes checks for a user restricted endpoint")
      val httpRequest = Http(s"$apiGatewayUrl/api-simulator/user-restricted-version-2-0-endpoint")
        .header(ACCEPT, "application/vnd.hmrc.2.0+json")
        .header(AUTHORIZATION, "Bearer 80d964331707baf8872179c805352")
      mockWsClient(wsClient, "http://ad.example:9001/api-definition?context=api-simulator", OK, loadStubbedJson("api-definition/api-simulator"))
      mockWsClient(wsClient, "http://tpda.example:9002/authority?access_token=80d964331707baf8872179c805352", OK, loadStubbedDelegatedAuthority("80d964331707baf8872179c805352"))

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the http response is '200' ok")
      assertCodeIs(httpResponse, OK)

      And("""the response message is '{"message":"response from /user-restricted-version-2-0-endpoint"}'""")
      assertBodyIs(httpResponse, """ {"message":"response from /user-restricted-version-2-0-endpoint"} """)
    }

    // TODO
    scenario("a request passing checks for an open endpoint is proxied") {
      pending
      Given("a request which passes checks for an open endpoint")
      val httpRequest = Http(s"$apiGatewayUrl/api-simulator/open-version-2-0-endpoint")
        .header(ACCEPT, "application/vnd.hmrc.2.0+json")

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the http response is '200' ok")
      assertCodeIs(httpResponse, OK)

      And("""the response message is '{"message":"Hello World"}'""")
      assertBodyIs(httpResponse, """ {"message":"Hello World"} """)
    }
  }

  private def loadStubbedDelegatedAuthority(accessToken: String): String = {
    Option(loadStubbedJson(s"authority/$accessToken")).map(parse(_).as[Authority]) match {
      case Some(authority) =>
        if (authority.authExpired) stringify(toJson(authority))
        else {
          val validToken: Token = authority.delegatedAuthority.token.copy(expiresAt = DateTime.now().plusMinutes(5))
          val validTpda: ThirdPartyDelegatedAuthority = authority.delegatedAuthority.copy(token = validToken)
          stringify(toJson(authority.copy(delegatedAuthority = validTpda)))
        }
      case _ => throw NotFound()
    }
  }

}
