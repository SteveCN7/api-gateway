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
import org.joda.time.DateTimeUtils.{setCurrentMillisFixed, setCurrentMillisSystem}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.mvc.{Headers, RequestHeader, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError.MatchingResourceNotFound
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.model.{ApiDefinitionMatch, AuthType, ProxyRequest}
import uk.gov.hmrc.apigateway.service.EndpointService
import uk.gov.hmrc.apigateway.util.RequestTags._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class GenericEndpointFilterSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  implicit val executionContextExecutor = ExecutionContext.Implicits.global
  implicit val materializer = mock[Materializer]

  trait Setup {
    val endpointService = mock[EndpointService]
    val genericEndpointFilter = new GenericEndpointFilter(endpointService)
  }

  override def beforeEach() {
    setCurrentMillisFixed(10000)
  }

  override def afterEach() {
    setCurrentMillisSystem()
  }

  val apiDefinitionMatch = ApiDefinitionMatch("foo", "http://api.service", "1.0", NONE, None)

  "Generic endpoint filter" should {

    val fakeRequest = FakeRequest("GET", "/foo/path",
      Headers("Accept" -> "application/vnd.hmrc.1.0+json"), """{"request":"json"}""")

    val nextFilter: (RequestHeader) => Future[Result] = { requestHeader => successful(Ok("""{"response":"json"}""")) }
    def returnAllTags(): (RequestHeader) => Future[Result] = {requestHeader =>
      successful(Ok(Json.toJson(requestHeader.tags)))
    }

    "decline a request which fails endpoint match filter" in new Setup {
      when(endpointService.findApiDefinition(any[ProxyRequest])).thenThrow(MatchingResourceNotFound())

      intercept[MatchingResourceNotFound] {
        genericEndpointFilter(nextFilter)(fakeRequest)
      }
    }

    "process a request which meets all requirements" in new Setup {
      when(endpointService.findApiDefinition(any[ProxyRequest])).thenReturn(successful(apiDefinitionMatch))

      val result = await(genericEndpointFilter(nextFilter)(fakeRequest))

      status(result) shouldBe OK
      bodyOf(result) shouldBe """{"response":"json"}"""
    }

    "set the tags to the requestHeader" in new Setup {
      val timestampNanoBeforeExecution = System.nanoTime()
      val request = fakeRequest.copyFakeRequest(
        uri = "foo/path",
        headers = Headers("Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "Bearer 12345"))

      when(endpointService.findApiDefinition(any[ProxyRequest])).thenReturn(successful(apiDefinitionMatch))

      val tags = jsonBodyOf(await(genericEndpointFilter(returnAllTags())(request)))

      (tags \ API_CONTEXT).as[String] shouldBe "foo"
      (tags \ API_VERSION).as[String] shouldBe "1.0"
      (tags \ API_ENDPOINT).as[String] shouldBe "http://api.service/foo/path"
      (tags \ API_SCOPE).asOpt[String] shouldBe None
      (tags \ AUTH_TYPE).as[String] shouldBe "NONE"
      (tags \ OAUTH_AUTHORIZATION).as[String] shouldBe "Bearer 12345"
      (tags \ REQUEST_TIMESTAMP_MILLIS).as[String] shouldBe "10000"
      val timestampNano = (tags \ REQUEST_TIMESTAMP_NANO).as[String]
      val timestampNanoAfterExecution = System.nanoTime()
      timestampNano.toLong should  (be > timestampNanoBeforeExecution and be < timestampNanoAfterExecution)
    }

    "set the tags to the requestHeader for User endpoint" in new Setup {
      val userRestrictedApi = apiDefinitionMatch.copy(authType = AuthType.USER, scope = Some("scope1"))
      when(endpointService.findApiDefinition(any[ProxyRequest])).thenReturn(successful(userRestrictedApi))

      val tags = jsonBodyOf(await(genericEndpointFilter(returnAllTags())(fakeRequest)))

      (tags \ API_SCOPE).asOpt[String] shouldBe Some("scope1")
      (tags \ AUTH_TYPE).as[String] shouldBe "USER"
    }

    "set the tags to the requestHeader for Application endpoint" in new Setup {
      val applicationRestrictedApi = apiDefinitionMatch.copy(authType = AuthType.APPLICATION)
      when(endpointService.findApiDefinition(any[ProxyRequest])).thenReturn(successful(applicationRestrictedApi))

      val tags = jsonBodyOf(await(genericEndpointFilter(returnAllTags())(fakeRequest)))

      (tags \ API_SCOPE).asOpt[String] shouldBe None
      (tags \ AUTH_TYPE).as[String] shouldBe "APPLICATION"
    }
  }

}
