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
