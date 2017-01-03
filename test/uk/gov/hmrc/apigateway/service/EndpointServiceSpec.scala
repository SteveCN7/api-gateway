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

package uk.gov.hmrc.apigateway.service

import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.apigateway.connector.impl.ApiDefinitionConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.{MatchingResourceNotFound, NotFound}
import uk.gov.hmrc.apigateway.model.AuthType.NONE
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.util.HttpHeaders.ACCEPT
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future._

class EndpointServiceSpec extends UnitSpec with MockitoSugar {

  private val apiDefinitionConnector = mock[ApiDefinitionConnector]
  private val endpointService = new EndpointService(apiDefinitionConnector)
  private val apiDefinition = ApiDefinition(
    "api-context", "http://host.example", Seq(ApiVersion("1.0", Seq(ApiEndpoint("/api-endpoint", "GET", NONE))))
  )

  "Endpoint service" should {

    val proxyRequest = ProxyRequest("GET", "/api-context/api-endpoint", headers = Map(ACCEPT -> "application/vnd.hmrc.1.0+json"))
    val apiDefinitionMatch = ApiDefinitionMatch("api-context", "http://host.example", "1.0", NONE, None)

    "invoke api definition connector with correct service name" in {
      mockApiServiceConnectorToReturnSuccess
      await(endpointService.findApiDefinition(proxyRequest))
      verify(apiDefinitionConnector).getByContext("api-context")
    }

    "return api definition when proxy request matches api definition endpoint" in {
      mockApiServiceConnectorToReturnSuccess
      await(endpointService.findApiDefinition(proxyRequest)) shouldBe apiDefinitionMatch
    }

    "fail with NotFound when no version matches the Accept headers in the API Definition" in {
      val request = proxyRequest.copy(headers = Map("Accept" -> "application/vnd.hmrc.55.0+json"))

      mockApiServiceConnectorToReturnSuccess

      intercept[NotFound]{await(endpointService.findApiDefinition(request))}
    }

    "fail with MatchingResourceNotFound when no endpoint matches in the API Definition" in {
      val request = proxyRequest.copy(path = "/api-context/invalidEndpoint")

      mockApiServiceConnectorToReturnSuccess

      intercept[MatchingResourceNotFound]{await(endpointService.findApiDefinition(request))}
    }

    "fail with MatchingResourceNotFound when a required request parameter is not in the URL" in {
      val request = proxyRequest.copy(path = "/api-context/api-endpoint")

      val anApiDefinition = ApiDefinition("api-context", "http://host.example", Seq(ApiVersion("1.0",
        Seq(ApiEndpoint("/api-endpoint", "GET", NONE, queryParameters = Some(Seq(Parameter("requiredParam", required = true))))))))

      mockApiServiceConnectorToReturn("api-context", successful(anApiDefinition))

      intercept[MatchingResourceNotFound]{await(endpointService.findApiDefinition(request))}
    }

    "succeed when all required request parameter are in the URL" in {
      val request = proxyRequest.copy(queryParameters = Map("requiredParam" -> Seq("test")))

      val anApiDefinition = ApiDefinition("api-context", "http://host.example", Seq(ApiVersion("1.0",
        Seq(ApiEndpoint("/api-endpoint", "GET", NONE, queryParameters = Some(Seq(Parameter("requiredParam", required = true))))))))

      mockApiServiceConnectorToReturn("api-context", successful(anApiDefinition))

      await(endpointService.findApiDefinition(request)) shouldBe apiDefinitionMatch
    }

    "throw an exception when proxy request does not match api definition endpoint" in {
      mockApiServiceConnectorToReturnFailure
      intercept[RuntimeException] {
        await(endpointService.findApiDefinition(proxyRequest))
      }
    }

  }

  private def mockApiServiceConnectorToReturnSuccess =
    mockApiServiceConnectorToReturn("api-context", successful(apiDefinition))

  private def mockApiServiceConnectorToReturnFailure =
    mockApiServiceConnectorToReturn("api-context", failed(new RuntimeException("simulated test exception")))

  private def mockApiServiceConnectorToReturn(context: String, eventualApiDefinition: Future[ApiDefinition]) =
    when(apiDefinitionConnector.getByContext(context)).thenReturn(eventualApiDefinition)

}
