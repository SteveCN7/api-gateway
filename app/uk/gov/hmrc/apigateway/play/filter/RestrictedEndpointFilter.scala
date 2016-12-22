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

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import play.api.mvc._
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.exception.GatewayError.{NotFound => _}
import uk.gov.hmrc.apigateway.model.ProxyRequest
import uk.gov.hmrc.apigateway.util.HttpHeaders._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

/**
  * Filter for inspecting requests for restricted endpoints and
  * evaluating their eligibility to be proxied to a downstream services.
  */
@Singleton
class RestrictedEndpointFilter @Inject()
(delegatedAuthorityFilter: DelegatedAuthorityFilter, scopeValidationFilter: ScopeValidationFilter)
(implicit override val mat: Materializer, executionContext: ExecutionContext) extends Filter {

  override def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader) = {
    val proxyRequest = ProxyRequest(requestHeader)

    val eventualRequestHeader = requestHeader.tags.get(X_API_GATEWAY_AUTH_TYPE) match {
      case Some(authType) if authType != "NONE" => for {
        delegatedAuthority <- delegatedAuthorityFilter.filter(proxyRequest)
        isValidScope <- scopeValidationFilter.filter(delegatedAuthority, requestHeader.tags.get(X_API_GATEWAY_SCOPE))
      // TODO implement rate limit filter based on api definition
      // TODO implement subscription filter
      // TODO implement token swap
      } yield requestHeader
      case _ => successful(requestHeader)
    }

    eventualRequestHeader.flatMap(nextFilter) recover GatewayError.recovery
  }

}
