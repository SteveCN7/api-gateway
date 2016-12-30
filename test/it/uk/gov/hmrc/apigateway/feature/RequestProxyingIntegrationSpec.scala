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
import play.api.http.Status._
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.util.HttpHeaders.ACCEPT

import scalaj.http.Http

class RequestProxyingIntegrationSpec extends BaseFeatureSpec {

  val anApiDefinition = ApiDefinition("api-simulator", api.url,
    Seq(
      ApiVersion("1.0", Seq(ApiEndpoint("version1", "GET", AuthType.NONE))),
      ApiVersion("2.0", Seq(ApiEndpoint("version2", "GET", AuthType.NONE,
        queryParameters = Some(Seq(Parameter("requiredParam", required = true), Parameter("optionalParam", required = false))))))
    ))
  val apiResponse = """{"response": "ok"}"""

  override def beforeEach() {
    super.beforeEach()

    Given("An API Definition exists")
    apiDefinition.willReturnTheApiDefinition(anApiDefinition)

    And("The API returns a response")
    api.willReturnTheResponse(apiResponse)
  }

  feature("The API gateway proxies requests to downstream services") {

    scenario("A request without an 'accept' http header is proxied to the version 1.0") {
      Given("A request without an 'accept' http header")
      val httpRequest = Http(s"$serviceUrl/api-simulator/version1")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The request is proxied")
      assertCodeIs(httpResponse, OK)
      assertBodyIs(httpResponse, apiResponse)
    }

    scenario("A request with a malformed 'accept' http header is proxied to the version 1.0") {
      Given("A request with a malformed 'accept' http header")
      val httpRequest = Http(s"$serviceUrl/api-simulator/version1").header(ACCEPT, "application/json")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The request is proxied")
      assertCodeIs(httpResponse, OK)
      assertBodyIs(httpResponse, apiResponse)
    }

    scenario("A request whose context cannot be matched is not proxied") {
      Given("A request with an invalid context")
      val httpRequest = Http(s"$serviceUrl/foo").header(ACCEPT, "application/vnd.hmrc.1.0+json")

      And("No API matches the context")
      apiDefinition.willNotReturnAnApiDefinitionForContext("foo")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '404' not found")
      assertCodeIs(httpResponse, NOT_FOUND)

      And("The response message code is 'NOT_FOUND'")
      assertBodyIs(httpResponse, """ {"code":"NOT_FOUND","message":"The requested resource could not be found."} """)
    }

    scenario("A request whose resource cannot be matched is not proxied") {

      Given("A request with an invalid resource")
      val httpRequest = Http(s"$serviceUrl/api-simulator/non-existent-resource").header(ACCEPT, "application/vnd.hmrc.1.0+json")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '404' not found")
      assertCodeIs(httpResponse, NOT_FOUND)

      And("The response message code is 'MATCHING_RESOURCE_NOT_FOUND'")
      assertBodyIs(httpResponse,
        """{
          "code":"MATCHING_RESOURCE_NOT_FOUND",
          "message":"A resource with the name in the request cannot be found in the API"
          } """)
    }

    scenario("A request whose version cannot be matched is not proxied") {
      Given("A request without a non existent version")
      val httpRequest = Http(s"$serviceUrl/api-simulator/version1").header(ACCEPT, "application/vnd.hmrc.3.0+json")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '404' not found")
      assertCodeIs(httpResponse, NOT_FOUND)

      And("The response message code is 'NOT_FOUND'")
      assertBodyIs(httpResponse,
        """{
          "code":"NOT_FOUND",
          "message":"The requested resource could not be found."
          } """)
    }

    scenario("A request without a mandatory request parameter is not proxied") {
      Given("A request without a mandatory request parameter")
      val httpRequest = Http(s"$serviceUrl/api-simulator/version2").header(ACCEPT, "application/vnd.hmrc.2.0+json")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '404' not found")
      assertCodeIs(httpResponse, NOT_FOUND)

      And("The response message code is 'NOT_FOUND'")
      assertBodyIs(httpResponse,
        """{
          "code":"MATCHING_RESOURCE_NOT_FOUND",
          "message":"A resource with the name in the request cannot be found in the API"
          } """)
    }

    scenario("A request with all mandatory request parameters is proxied") {
      Given("A request with the mandatory request parameter")
      val httpRequest = Http(s"$serviceUrl/api-simulator/version2?requiredParam=test").header(ACCEPT, "application/vnd.hmrc.2.0+json")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The request is proxied")
      assertCodeIs(httpResponse, OK)
      assertBodyIs(httpResponse, apiResponse)
    }
  }
}
