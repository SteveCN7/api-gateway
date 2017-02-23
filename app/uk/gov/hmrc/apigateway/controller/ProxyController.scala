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

import play.api.Logger
import play.api.http.Status.{BAD_GATEWAY, GATEWAY_TIMEOUT, NOT_IMPLEMENTED, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, BodyParsers, Result}
import play.api.mvc.Results.{NotFound => PlayNotFound, _}
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._
import uk.gov.hmrc.apigateway.service.{AuditService, ProxyService, RoutingService}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ProxyController @Inject()(proxyService: ProxyService, routingService: RoutingService, auditService: AuditService) {

  private def newResult: (Result, Result) => Result = { (originalResult, newResult) =>
    Logger.warn(s"Api Gateway is converting a ${originalResult.header.status} response to ${newResult.header.status}")
    newResult
  }

  private def transformError: Result => Result = {
    result => result.header.status match {
      case NOT_IMPLEMENTED => newResult(result, NotImplemented(toJson(GatewayError.NotImplemented())))
      case BAD_GATEWAY | SERVICE_UNAVAILABLE | GATEWAY_TIMEOUT => newResult(result, ServiceUnavailable(toJson(GatewayError.ServiceUnavailable())))
      case _ => result
    }
  }

  def recoverError: PartialFunction[Throwable, Result] = {
    case e: GatewayError.MissingCredentials =>
      auditService.auditFailingRequest(e.request, e.apiRequest)
      Unauthorized(toJson(e))
    case e: GatewayError.InvalidCredentials =>
      auditService.auditFailingRequest(e.request, e.apiRequest)
      Unauthorized(toJson(e))
    case e: GatewayError.IncorrectAccessTokenType => Unauthorized(toJson(e))

    case e: GatewayError.InvalidScope => Forbidden(toJson(e))
    case e: GatewayError.InvalidSubscription => Forbidden(toJson(e))

    case e: GatewayError.MatchingResourceNotFound => PlayNotFound(toJson(e))
    case e: GatewayError.NotFound => PlayNotFound(toJson(e))

    case e: GatewayError.ThrottledOut => TooManyRequests(toJson(e))

    case e =>
      Logger.error("unexpected error", e)
      InternalServerError(toJson(GatewayError.ServerError()))
  }

  def proxy = Action.async(BodyParsers.parse.anyContent) { implicit request =>
    routingService.routeRequest(request) flatMap { apiRequest =>
      proxyService.proxy(request, apiRequest)
    } recover recoverError map transformError
  }

}
