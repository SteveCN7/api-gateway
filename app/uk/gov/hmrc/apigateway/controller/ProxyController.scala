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

package uk.gov.hmrc.apigateway.controller

import javax.inject.{Inject, Singleton}

import play.Logger
import play.api.http.Status._
import play.api.libs.json.Json.toJson
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model.ProxyRequest
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._
import uk.gov.hmrc.apigateway.service.{ProxyService, RoutingService}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ProxyController @Inject()(proxyService: ProxyService, routingService: RoutingService) {

  private def transformError: Result => Result = {
    res => res.header.status match {
      case NOT_IMPLEMENTED => NotImplemented(toJson("API has not been implemented"))
      case BAD_GATEWAY | SERVICE_UNAVAILABLE | GATEWAY_TIMEOUT => ServiceUnavailable(toJson("Service unavailable"))
      case _ => res
    }
  }

  private def recoverError: PartialFunction[Throwable, Result] = {
    case e =>
      Logger.error("unexpected error", e)
      InternalServerError(toJson(ServerError()))
  }

  def proxy = Action.async(BodyParsers.parse.anyContent) { implicit request =>
    routingService.routeRequest(ProxyRequest(request)) flatMap { apiReq =>
      proxyService.proxy(request, apiReq)
    } recover GatewayError.recovery recover recoverError map transformError
  }

}
