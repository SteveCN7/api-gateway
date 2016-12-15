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

package uk.gov.hmrc.play.filter

import akka.stream.Materializer
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.model.{ApiDefinitionMatch, ProxyRequest}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class ProxyFilterSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    implicit val materializer = mock[Materializer]
    val endpointMatchFilter = mock[EndpointMatchFilter]
    val proxyFilter = new ProxyFilter(endpointMatchFilter)(materializer, ExecutionContext.Implicits.global)
  }

  "Proxy filter" should {

    val nextFilterFunction: (RequestHeader) => Future[Result] = { requestHeader => successful(Ok) }
    val fakeRequest = FakeRequest("GET", "http://host.example/foo", Headers("" -> ""), "{}")

    "propagate endpoint match filter failures" in new MockedSetup {
      mockEndpointMatchFilterToThrow(new RuntimeException("simulated test exception"))
      intercept[RuntimeException] {
        proxyFilter(nextFilterFunction)(fakeRequest)
      }.getMessage shouldBe "simulated test exception"
    }

    "propagate accept header to next filter" in new MockedSetup {
      pending
      val apiDefinitionMatch = ApiDefinitionMatch("foo", "http://host.example", "1,0", None)
      mockEndpointMatchFilterToReturn(successful(apiDefinitionMatch))
      val nextFilterFunction = mock[(RequestHeader) => Future[Result]]

      proxyFilter(nextFilterFunction)(fakeRequest)

      val requestHeaderArgumentCaptor = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(nextFilterFunction).apply(requestHeaderArgumentCaptor.capture())
      requestHeaderArgumentCaptor.getValue
    }

  }

  trait MockedSetup extends Setup {
    def mockEndpointMatchFilterToThrow(throwable: Throwable) =
      when(endpointMatchFilter.filter(any[ProxyRequest])).thenThrow(throwable)

    def mockEndpointMatchFilterToReturn(eventualApiDefinitionMatch: Future[ApiDefinitionMatch]) =
      when(endpointMatchFilter.filter(any[ProxyRequest])).thenReturn(eventualApiDefinitionMatch)
  }

}
