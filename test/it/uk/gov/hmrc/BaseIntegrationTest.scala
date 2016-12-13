package it.uk.gov.hmrc

import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen, Matchers}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json._
import play.api.test.TestServer

import scalaj.http.{HttpRequest, HttpResponse}

abstract class BaseIntegrationTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with Matchers {

  protected lazy val testServer = TestServer(9999, GuiceApplicationBuilder().build())
  protected val apiGatewayUrl = "http://localhost:9999/api-gateway"

  override protected def beforeAll() = testServer.start()

  protected def invoke(httpRequest: HttpRequest): HttpResponse[String] =
    httpRequest.asString

  protected def assertCodeIs(httpResponse: HttpResponse[String], expectedHttpCode: Int) =
    httpResponse.code shouldBe expectedHttpCode

  protected def assertBodyIs(httpResponse: HttpResponse[String], expectedJsonBody: String) =
    parse(httpResponse.body) shouldBe parse(expectedJsonBody)

  override protected def afterAll() = testServer.stop()

}
