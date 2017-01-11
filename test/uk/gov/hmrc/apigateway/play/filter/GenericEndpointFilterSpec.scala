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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.mvc.Results.Ok
import play.api.mvc.{Headers, RequestHeader, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError.MatchingResourceNotFound
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.model.{ApiDefinitionMatch, ProxyRequest}
import uk.gov.hmrc.apigateway.service.EndpointService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class GenericEndpointFilterSpec extends UnitSpec with MockitoSugar {

  implicit val executionContextExecutor = ExecutionContext.Implicits.global
  implicit val materializer = mock[Materializer]

  trait Setup {
    val endpointService = mock[EndpointService]
    val genericEndpointFilter = new GenericEndpointFilter(endpointService)
  }

  "Generic endpoint filter" should {

    val fakeRequest = FakeRequest("GET", "http://host.example/foo", Headers("" -> ""), """{"request":"json"}""")
    val nextFilter: (RequestHeader) => Future[Result] = { requestHeader => successful(Ok("""{"response":"json"}""")) }

    "decline a request which fails endpoint match filter" in new Setup {
      when(endpointService.findApiDefinition(any[ProxyRequest])).thenThrow(MatchingResourceNotFound())
      intercept[MatchingResourceNotFound] {
        genericEndpointFilter(nextFilter)(fakeRequest)
      }
    }

    "process a request which meets all requirements" in new Setup {
      val apiDefinitionMatch = ApiDefinitionMatch("foo", "http://host.example", "1,0", NONE, None)
      when(endpointService.findApiDefinition(any[ProxyRequest])).thenReturn(successful(apiDefinitionMatch))

      val result = await(genericEndpointFilter(nextFilter)(fakeRequest))
      status(result) shouldBe OK
      bodyOf(result) shouldBe """{"response":"json"}"""
    }
  }

}
