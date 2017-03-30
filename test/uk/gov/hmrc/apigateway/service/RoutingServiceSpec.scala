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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.exception.GatewayError.{MatchingResourceNotFound, ServerError}
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.{failed, successful}

class RoutingServiceSpec extends UnitSpec with MockitoSugar {

  private val openRequest = FakeRequest("GET", "/hello/world")
  private val openApiRequest = ApiRequest(
    apiIdentifier = ApiIdentifier("foo1", "1.0"),
    apiEndpoint = "http://api.service1//hello/world")

  private val userRequest = FakeRequest("GET", "/hello/user")
  private val userApiRequest = ApiRequest(
    apiIdentifier = ApiIdentifier("foo2", "2.0"),
    authType = USER,
    apiEndpoint = "http://api.service2//hello/user",
    scope = Some("scope2"))

  private val applicationRequest = FakeRequest("GET", "/hello/application")
  private val applicationApiRequest = ApiRequest(
    apiIdentifier = ApiIdentifier("foo3", "3.0"),
    authType = APPLICATION,
    apiEndpoint = "http://api.service3//hello/application")

  private trait Setup {
    val endpointService = mock[EndpointService]
    val userRestrictedEndpointService = mock[UserRestrictedEndpointService]
    val applicationRestrictedEndpointService = mock[ApplicationRestrictedEndpointService]

    val routingService = new RoutingService(endpointService, userRestrictedEndpointService, applicationRestrictedEndpointService)
  }

  "routeRequest" should {

    "decline an open-endpoint request which fails endpoint match filter" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(failed(MatchingResourceNotFound()))

      intercept[MatchingResourceNotFound] {
        await(routingService.routeRequest(openRequest))
      }
    }

    "route an open-endpoint request" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(successful(openApiRequest))

      val apiRequest = await(routingService.routeRequest(openRequest))

      apiRequest shouldBe openApiRequest
    }

    "decline a user-endpoint request which fails to route" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(successful(userApiRequest))
      when(userRestrictedEndpointService.routeRequest(userRequest, ProxyRequest(userRequest), userApiRequest))
        .thenReturn(failed(ServerError()))

      intercept[ServerError] {
        await(routingService.routeRequest(userRequest))
      }
    }

    "route a user-endpoint request" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(successful(userApiRequest))
      when(userRestrictedEndpointService.routeRequest(userRequest, ProxyRequest(userRequest), userApiRequest))
        .thenReturn(successful(userApiRequest))

      val apiRequest = await(routingService.routeRequest(userRequest))

      apiRequest shouldBe userApiRequest
    }

    "decline an application-endpoint request which fails to route" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(successful(applicationApiRequest))
      when(applicationRestrictedEndpointService.routeRequest(applicationRequest, ProxyRequest(applicationRequest), applicationApiRequest))
        .thenReturn(failed(ServerError()))

      intercept[ServerError] {
        await(routingService.routeRequest(applicationRequest))
      }
    }

    "route an application-endpoint request" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(successful(applicationApiRequest))
      when(applicationRestrictedEndpointService.routeRequest(applicationRequest, ProxyRequest(applicationRequest), applicationApiRequest))
        .thenReturn(successful(applicationApiRequest))

      val apiRequest = await(routingService.routeRequest(applicationRequest))

      apiRequest shouldBe applicationApiRequest
    }
  }

}
