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

package uk.gov.hmrc.apigateway.play.handler

import java.util.UUID
import javax.inject.{Inject, Singleton}

import play.api.http.{DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router
import uk.gov.hmrc.apigateway.controller.ProxyController

@Singleton
class ProxyRequestHandler @Inject()
(errorHandler: HttpErrorHandler,
 configuration: HttpConfiguration,
 filters: HttpFilters,
 proxyRoutes: Router,
 proxyController: ProxyController)
  extends DefaultHttpRequestHandler(proxyRoutes, errorHandler, configuration, filters) {

  override def handlerForRequest(requestHeader: RequestHeader): (RequestHeader, Handler) = {
    health.Routes.routes.lift(requestHeader) match {
      case Some(handler) => (requestHeader, handler)
      case _ => super.handlerForRequest(requestHeader)
    }
  }

  override def routeRequest(requestHeader: RequestHeader): Option[Handler] = {
    implicit val requestId = UUID.randomUUID().toString
    Some(proxyController.proxy)
  }
}
