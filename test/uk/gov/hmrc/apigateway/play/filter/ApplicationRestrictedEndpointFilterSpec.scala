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

package uk.gov.hmrc.apigateway.play.filter

import akka.stream.Materializer
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Headers
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.service.{ApplicationService, AuthorityService}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.apigateway.util.RequestTags.{API_CONTEXT, API_VERSION, AUTH_TYPE, CLIENT_ID}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.{failed, successful}
import scala.concurrent.ExecutionContext

class ApplicationRestrictedEndpointFilterSpec extends UnitSpec with MockitoSugar with EndpointFilterMocking {

  implicit val executionContextExecutor = ExecutionContext.Implicits.global
  implicit val materializer = mock[Materializer]

  private trait Setup {
    val authorityService = mock[AuthorityService]
    val applicationService = mock[ApplicationService]
    val underTest = new ApplicationRestrictedEndpointFilter(authorityService, applicationService)

    val serverToken = "accessToken"
    val clientId = "clientId"
    val application = anApplication()

    val basicRequest = new FakeRequest(
      method = "GET",
      uri = "http://host.example/foo",
      headers = Headers(),
      body = "")
    val applicationRequest = basicRequest
      .withTag(AUTH_TYPE, APPLICATION.toString)
      .withTag(API_CONTEXT, "c")
      .withTag(API_VERSION, "v")
    val applicationRequestWithToken = applicationRequest.copy(headers = Headers(AUTHORIZATION -> s"Bearer $serverToken"))

    mockValidateRateLimit(applicationService, application, successful(()))
  }

  "Application restricted endpoint filter" should {

    "fail with a request not matching authority" in new Setup {
      intercept[MissingCredentials] {
        await(underTest.filter(applicationRequest, ProxyRequest(applicationRequest)))
      }
    }

    "propagate the error, when there is a failure in fetching the application by server token" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, ServerError())

      intercept[ServerError] {
        await(underTest.filter(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken)))
      }
    }

    "fail, with a request without a valid access token" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, NotFound())
      mockAuthority(authorityService, NotFound())

      intercept[InvalidCredentials] {
        await(underTest.filter(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken)))
      }
    }

    "propagate the error, when there is a failure in fetching the application by client id" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, NotFound())
      mockAuthority(authorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, ServerError())

      intercept[ServerError] {
        await(underTest.filter(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken)))
      }
    }

    "propagate the error, when there is a failure in finding the application subscriptions" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, NotFound())
      mockAuthority(authorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockApiSubscriptions(applicationService, ServerError())

      intercept[ServerError] {
        await(underTest.filter(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken)))
      }
    }

    "fail with ThrottledOut when the application rate limit has been reached" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, NotFound())
      mockAuthority(authorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockApiSubscriptions(applicationService)
      mockValidateRateLimit(applicationService, application, failed(ThrottledOut()))

      intercept[ThrottledOut] {
        await(underTest.filter(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken)))
      }
    }

    "process a request with a valid access token that meets all requirements" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, NotFound())
      mockAuthority(authorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockApiSubscriptions(applicationService)

      val result = await(underTest.filter(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken)))
      result.headers shouldBe applicationRequestWithToken.headers
      result.tags(CLIENT_ID) shouldBe clientId
    }

    "process a request with a valid server token that meets all requirements" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, application)
      mockApiSubscriptions(applicationService)

      val result = await(underTest.filter(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken)))
      result.headers shouldBe applicationRequestWithToken.headers
      result.tags(CLIENT_ID) shouldBe clientId
    }

  }

}
