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

import javax.inject.Singleton

import play.api.http.HttpErrorHandler
import play.api.libs.json.Json._
import play.api.mvc.Results.ServiceUnavailable
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.apigateway.exception.GatewayError.ServiceNotAvailable
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._

import scala.concurrent.Future

//TODO: remove `ServiceNotAvailable()` errors when WSO2 has been decommissioned

@Singleton
class ErrorHandler extends HttpErrorHandler{
  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(ServiceUnavailable(toJson(ServiceNotAvailable())))
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(ServiceUnavailable(toJson(ServiceNotAvailable())))
  }
}
