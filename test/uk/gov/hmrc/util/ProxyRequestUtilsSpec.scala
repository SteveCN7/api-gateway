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

package uk.gov.hmrc.util

import uk.gov.hmrc.exception.GatewayError.{ContextNotFound, InvalidAcceptHeader}
import uk.gov.hmrc.model.ProxyRequest
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.util.HttpHeaders.ACCEPT
import uk.gov.hmrc.util.ProxyRequestUtils.{validateContext, validateVersion}

class ProxyRequestUtilsSpec extends UnitSpec {

  private val proxyRequest = ProxyRequest("", "")

  "Request context validation" should {

    "fail for request without context" in {
      intercept[ContextNotFound] {
        await(validateContext(proxyRequest.copy(path = "")))
      }
    }

    "succeed for request with context" in {
      await(validateContext(proxyRequest.copy(path = "/foo/bar"))) shouldBe "foo"
    }

  }

  "Request version validation" should {

    def runTestWithHeaderFixture(headerFixture: Map[String, String]): String = {
      await(validateVersion(proxyRequest.copy(headers = headerFixture)))
    }

    "fail for request without correct accept header" in {
      def runTestWithHeaderFixtureAndInterceptException(headersFixtures: Map[String, String]*) = {
        headersFixtures.foreach { headersFixture =>
          intercept[InvalidAcceptHeader] {
            runTestWithHeaderFixture(headersFixture)
          }
        }
      }

      runTestWithHeaderFixtureAndInterceptException(
        Map.empty,
        Map(ACCEPT -> "foo/bar"),
        Map(ACCEPT -> "application/vnd.hmrc.1.0"))
    }

    "succeed for request with correct accept header" in {
      runTestWithHeaderFixture(Map(ACCEPT -> "application/vnd.hmrc.1.0+json")) shouldBe "1.0"
    }

  }

}
