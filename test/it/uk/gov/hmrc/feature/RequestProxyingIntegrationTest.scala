package it.uk.gov.hmrc.feature

import it.uk.gov.hmrc.BaseIntegrationTest
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND}
import uk.gov.hmrc.util.HttpHeaders.ACCEPT

import scalaj.http.Http

class RequestProxyingIntegrationTest extends BaseIntegrationTest {

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
      Given("a request without a non existent context")
      val httpRequest = Http(s"$apiGatewayUrl/foo").header(ACCEPT, "application/vnd.hmrc.1.0+json")

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the http response is '404' not found")
      assertCodeIs(httpResponse, NOT_FOUND)

      And("the response message code is 'NOT_FOUND'")
      assertBodyIs(httpResponse, """ {"code":"NOT_FOUND","message":"Requested resource could not be found"} """)
    }

    scenario("a request whose resource cannot be matched is not proxied") {
      Given("a request without a '/non-existent' resource")
      val httpRequest = Http(s"$apiGatewayUrl/api-simulator/non-existent-resource").header(ACCEPT, "application/vnd.hmrc.1.0+json") // TODO may have to mock this service!

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("the http response is '404' not found")
      assertCodeIs(httpResponse, NOT_FOUND)

      And("the response message code is 'MATCHING_RESOURCE_NOT_FOUND'")
      assertBodyIs(httpResponse, """ {"code":"MATCHING_RESOURCE_NOT_FOUND","message":"A resource with the name in the request cannot be found in the API"} """)
    }

  }

}
