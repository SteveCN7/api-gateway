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

package it.uk.gov.hmrc.apigateway.feature

import com.google.common.net.{HttpHeaders => http}
import it.uk.gov.hmrc.apigateway.BaseFeatureSpec
import it.uk.gov.hmrc.apigateway.testutils.RequestUtils
import play.api.http.Status._
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import play.api.http.HttpVerbs._

import scalaj.http.{HttpRequest, Http}

class RequestProxyingIntegrationSpec extends BaseFeatureSpec with RequestUtils {

  val anApiDefinition = ApiDefinition("api-simulator", api.url,
    Seq(
      ApiVersion("1.0",
        Seq(ApiEndpoint("version1", "GET", AuthType.NONE),
          ApiEndpoint("version1", "POST", AuthType.NONE),
          ApiEndpoint("version1", "PUT", AuthType.NONE),
          ApiEndpoint("version1", "PATCH", AuthType.NONE))),
      ApiVersion("2.0",
        Seq(ApiEndpoint("version2", "GET", AuthType.NONE,
        queryParameters = Some(Seq(
          Parameter("requiredParam", required = true),
          Parameter("optionalParam", required = false))))))
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

  feature("The API gateway is backward compatible with WSO2") {

    val verbs = Seq(POST, PUT, PATCH)
    val verbNames = verbs.mkString(",")

    def invokeAndValidateResponse(httpRequest: HttpRequest) = {

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '503' service unavailable")
      assertCodeIs(httpResponse, SERVICE_UNAVAILABLE)

      And("The response message 'SERVICE_UNAVAILABLE'")
      assertBodyIs(httpResponse, """{ "code": "SERVER_ERROR", "message": "Service unavailable"}""")
    }

    def request(verb: String, data: String): HttpRequest = {
      val http = Http(s"$serviceUrl/api-simulator/version1")

      verb match {
        case `POST` => http.postData(data)
        case `PUT` => http.put(data)
        case `PATCH` => http.put(data).method(PATCH)
        case _ => http
      }
    }

    scenario("A successful response for a proxied request matches WSO2 response headers") {
      Given("A valid request")
      val httpRequest = Http(s"$serviceUrl/api-simulator/version1")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The request is proxied successfully")
      assertCodeIs(httpResponse, OK)

      And("The response headers are backward compatible with WSO2")
      validateHeaders(flattenHeaders(httpResponse.headers),
        (TRANSFER_ENCODING, Some("chunked")),
        (VARY, Some("Accept")),
        (CONTENT_LENGTH, None),
        (http.STRICT_TRANSPORT_SECURITY, None),
        (http.X_FRAME_OPTIONS, None),
        (http.X_CONTENT_TYPE_OPTIONS, None))
    }

    scenario("A failed response for a request which cannot be proxied matches WSO2 response headers") {
      Given("A request for a non existent version")
      val httpRequest = Http(s"$serviceUrl/api-simulator/version1").header(ACCEPT, "application/vnd.hmrc.3.0+json")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '404' not found")
      assertCodeIs(httpResponse, NOT_FOUND)

      And("The response headers are backward compatible with WSO2")
      validateHeaders(flattenHeaders(httpResponse.headers),
        (CACHE_CONTROL, Some("no-cache")),
        (CONTENT_TYPE, Some("application/json; charset=UTF-8")),
        (http.X_FRAME_OPTIONS, None),
        (http.X_CONTENT_TYPE_OPTIONS, None))
    }

    scenario(s"$verbNames requests with an empty body fail with a WSO2 matching response") {

      def performTest(verb: String) = {
        Given(s"A $verb request with an empty body")
        val aRequest = request(verb, "")
          .header(CONTENT_TYPE, "application/json")
          .header(ACCEPT, "application/vnd.hmrc.1.0+json")

        withClue(s"A $verb request with an empty body was incorrectly proxied") {
          invokeAndValidateResponse(aRequest)
        }
      }

      verbs foreach (performTest)
    }

    scenario(s"$verbNames requests with an invalid body fail with a WSO2 matching response") {

      def performTest(verb: String) = {
        Given(s"A $verb request with an empty body")
        val aRequest = request(verb, "</html>")
          .header(CONTENT_TYPE, "application/json")
          .header(ACCEPT, "application/vnd.hmrc.1.0+json")

        withClue(s"A $verb request with an invalid body was incorrectly proxied") {
          invokeAndValidateResponse(aRequest)
        }
      }

      verbs foreach (performTest)
    }

    scenario(s"$verbNames requests without Content-Type fail with a WSO2 matching response") {

      def performTest(verb: String) = {
        Given(s"A $verb request with an empty body")
        val aRequest = request(verb, """{"foo":"bar"}""")
          .header(CONTENT_TYPE, "")
          .header(ACCEPT, "application/vnd.hmrc.1.0+json")

        withClue(s"A $verb request without Content-Type was incorrectly proxied") {
          invokeAndValidateResponse(aRequest)
        }
      }

      verbs foreach (performTest)
    }
  }
}
