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

package uk.gov.hmrc.apigateway.play.filter

import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.apigateway.connector.impl.ApiDefinitionConnector
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.apigateway.util.HttpHeaders.ACCEPT

import scala.concurrent.Future
import scala.concurrent.Future._

class EndpointMatchFilterSpec extends UnitSpec with MockitoSugar {

  private val apiDefinitionConnector = mock[ApiDefinitionConnector]
  private val endpointMatchFilter = new EndpointMatchFilter(apiDefinitionConnector)
  private val apiDefinition = ApiDefinition(
    "api-context", "http://host.example", Seq(ApiVersion("1.0", Seq(ApiEndpoint("/api-endpoint", "GET", "NONE"))))
  )

  "Endpoint match filter" should {

    val proxyRequest = ProxyRequest("GET", "/api-context/api-endpoint", Map(ACCEPT -> "application/vnd.hmrc.1.0+json"))
    val apiDefinitionMatch = ApiDefinitionMatch("api-context", "http://host.example", "1.0", "NONE", None)

    "invoke api definition connector with correct service name" in {
      mockApiServiceConnectorToReturnSuccess
      await(endpointMatchFilter.filter(proxyRequest))
      verify(apiDefinitionConnector).getByContext("api-context")
    }

    "return api definition when proxy request matches api definition endpoint" in {
      mockApiServiceConnectorToReturnSuccess
      await(endpointMatchFilter.filter(proxyRequest)) shouldBe apiDefinitionMatch
    }

    "throw an exception when proxy request does not match api definition endpoint" in {
      mockApiServiceConnectorToReturnFailure
      intercept[RuntimeException] {
        await(endpointMatchFilter.filter(proxyRequest))
      }
    }

  }

  private def mockApiServiceConnectorToReturnSuccess =
    mockApiServiceConnectorToReturn("api-context", successful(apiDefinition))

  private def mockApiServiceConnectorToReturnFailure =
    mockApiServiceConnectorToReturn("api-context", failed(new RuntimeException("simulated test exception")))

  private def mockApiServiceConnectorToReturn(serviceName: String, eventualApiDefinition: Future[ApiDefinition]) =
    when(apiDefinitionConnector.getByContext(serviceName)).thenReturn(eventualApiDefinition)

}
