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

package it.uk.gov.hmrc.apigateway

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import it.uk.gov.hmrc.apigateway.stubs.{ThirdPartyDelegatedAuthorityStub, ApiStub, ApiDefinitionStub}
import org.scalatest._
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.Json._

import scala.concurrent.duration._
import scalaj.http.{HttpResponse, HttpRequest}

abstract class BaseFeatureSpec extends FeatureSpec with GivenWhenThen with Matchers
with BeforeAndAfterEach with BeforeAndAfterAll with OneServerPerSuite {

  override lazy val port = 19111
  val serviceUrl = s"http://localhost:$port"
  val timeout = 10.second

  val apiDefinition = ApiDefinitionStub
  val api = ApiStub
  val thirdPartyDelegatedAuthority = ThirdPartyDelegatedAuthorityStub
  val mocks = Seq(apiDefinition, api, thirdPartyDelegatedAuthority)

  override protected def beforeEach(): Unit = {
    mocks.foreach(m => if (!m.stub.server.isRunning) m.stub.server.start())
  }

  override protected def afterEach(): Unit = {
    mocks.foreach(_.stub.mock.resetMappings())
  }

  override protected def afterAll(): Unit = {
    mocks.foreach(_.stub.server.stop())
  }

  protected def invoke(httpRequest: HttpRequest): HttpResponse[String] = httpRequest.asString

  protected def assertCodeIs(httpResponse: HttpResponse[String], expectedHttpCode: Int) =
    httpResponse.code shouldBe expectedHttpCode

  protected def assertBodyIs(httpResponse: HttpResponse[String], expectedJsonBody: String) =
    parse(httpResponse.body) shouldBe parse(expectedJsonBody)
}

case class MockHost(port: Int) {
  val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))
  val mock = new WireMock("localhost", port)
}

trait Stub {
  val stub: MockHost
}
