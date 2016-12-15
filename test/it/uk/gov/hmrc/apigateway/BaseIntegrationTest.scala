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
