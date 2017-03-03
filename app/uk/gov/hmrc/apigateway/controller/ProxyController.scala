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

import com.google.common.net.{HttpHeaders => http}
import play.Logger
import play.api.http.Status._
import play.api.libs.json.Json.toJson
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.model.ProxyRequest
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._
import uk.gov.hmrc.apigateway.service.{ProxyService, RoutingService}
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.apigateway.util.PlayRequestUtils

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ProxyController @Inject()(proxyService: ProxyService, routingService: RoutingService) {

  private def newResult: (Result, Result) => Result = {
    (originalResult, newResult) =>
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

  private def addHeaders(implicit requestId: String): Result => Result = {
    result =>
      val headers = PlayRequestUtils.replaceHeaders(Headers(result.header.headers.toSeq: _*))(
        (X_REQUEST_ID, Some(requestId)),
        (CACHE_CONTROL, Some("no-cache")),
        (CONTENT_TYPE, Some("application/json; charset=UTF-8")),
        (http.X_FRAME_OPTIONS, None),
        (http.X_CONTENT_TYPE_OPTIONS, None)
      ).toSimpleMap

      result.withHeaders(headers.toSeq: _*)
  }

  def proxy()(implicit requestId: String) = Action.async(BodyParsers.parse.anyContent) { implicit request =>
    routingService.routeRequest(ProxyRequest(request)) flatMap { apiRequest =>
      proxyService.proxy(request, apiRequest)
    } recover GatewayError.recovery.andThen(addHeaders) map transformError
  }
}
