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

package uk.gov.hmrc.apigateway.play.filter

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.model.ProxyRequest
import uk.gov.hmrc.play.health.routes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * A template filter providing the skeleton of a filter function in the API gateway.
  * This skeleton provides an invocation of a filter process and error handling around that.
  * Implementations should implement that filter process by overriding the [[ApiGatewayFilter#filter]] function.
  */
abstract class ApiGatewayFilter(implicit m: Materializer) extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader) = {
    filter(requestHeader, ProxyRequest(requestHeader)) flatMap nextFilter recover GatewayError.recovery
  }

  def filter(requestHeader: RequestHeader, proxyRequest: ProxyRequest): Future[RequestHeader]

}
