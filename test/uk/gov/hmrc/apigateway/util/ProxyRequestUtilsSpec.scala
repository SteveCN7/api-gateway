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

package uk.gov.hmrc.apigateway.util

import uk.gov.hmrc.apigateway.exception.GatewayError.NotFound
import uk.gov.hmrc.apigateway.model.ProxyRequest
import uk.gov.hmrc.apigateway.util.HttpHeaders.ACCEPT
import uk.gov.hmrc.apigateway.util.ProxyRequestUtils.{validateContext, parseVersion}
import uk.gov.hmrc.play.test.UnitSpec

class ProxyRequestUtilsSpec extends UnitSpec {

  private val proxyRequest = ProxyRequest("", "")

  "Request context validation" should {

    "fail for request without context" in {
      intercept[NotFound] {
        await(validateContext(proxyRequest.copy(path = "")))
      }
    }

    "succeed for request with context" in {
      await(validateContext(proxyRequest.copy(path = "/foo/bar"))) shouldBe "foo"
    }

  }

  "parseVersion" should {

    def runTestWithHeaderFixture(headerFixture: Map[String, String]): String = {
      await(parseVersion(proxyRequest.copy(headers = headerFixture)))
    }

    "return the default version 1.0 when the Accept header can not be parsed" in {
      def runTestWithHeaderFixtureAndInterceptException(headersFixtures: Map[String, String]*) = {
        headersFixtures.foreach { headersFixture =>
          runTestWithHeaderFixture(headersFixture) shouldBe "1.0"
        }
      }

      runTestWithHeaderFixtureAndInterceptException(
        Map.empty,
        Map(ACCEPT -> "foo/bar"),
        Map(ACCEPT -> "application/vnd.hmrc.aaa"))
    }

    "parse the version from the Accept header" in {
      runTestWithHeaderFixture(Map(ACCEPT -> "application/vnd.hmrc.2.0+json")) shouldBe "2.0"
    }

  }

}
