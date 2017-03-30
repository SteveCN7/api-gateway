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

import javax.inject.{Inject, Singleton}

import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class RoutingService @Inject()(endpointService: EndpointService,
                               userRestrictedEndpointService: UserRestrictedEndpointService,
                               applicationRestrictedEndpointService: ApplicationRestrictedEndpointService) {

  def routeRequest(request: Request[AnyContent]): Future[ApiRequest] = {
    val proxyRequest = ProxyRequest(request)
    val apiRequestF = endpointService.apiRequest(proxyRequest, request)
    apiRequestF flatMap { apiRequest =>
      apiRequest.authType match {
        case USER => userRestrictedEndpointService.routeRequest(request, proxyRequest, apiRequest)
        case APPLICATION => applicationRestrictedEndpointService.routeRequest(request, proxyRequest, apiRequest)
        case _ => apiRequestF
      }
    }
  }

}
