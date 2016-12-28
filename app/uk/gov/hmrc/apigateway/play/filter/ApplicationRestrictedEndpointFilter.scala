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
import uk.gov.hmrc.apigateway.exception.GatewayError.{MissingCredentials, NotFound => _}
import uk.gov.hmrc.apigateway.model.ProxyRequest
import uk.gov.hmrc.apigateway.util.HttpHeaders._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

/**
  * Filter for inspecting requests for application restricted endpoints and
  * evaluating their eligibility to be proxied to a downstream services.
  */
@Singleton
class ApplicationRestrictedEndpointFilter @Inject()
(delegatedAuthorityFilter: DelegatedAuthorityFilter)
(implicit override val mat: Materializer, executionContext: ExecutionContext) extends ApiGatewayFilter {

  override def filter(requestHeader: RequestHeader, proxyRequest: ProxyRequest): Future[RequestHeader] =
    requestHeader.tags.get(X_API_GATEWAY_AUTH_TYPE) match {
      case Some("APPLICATION") =>
        Try(delegatedAuthorityFilter.filter(proxyRequest)) match {
          case Success(eventualAuthority) => eventualAuthority map { authority =>
            requestHeader.withTag(X_APPLICATION_CLIENT_ID, authority.delegatedAuthority.token.accessToken)
          }
          case _ => requestHeader.tags.get(AUTHORIZATION) match {
            case Some(bearerToken) =>
              val serverToken = bearerToken.stripPrefix("Bearer ")
              successful(requestHeader.withTag(X_APPLICATION_CLIENT_ID, serverToken))
            case _ => failed(MissingCredentials())
          }
        }
      case _ => successful(requestHeader)
    }

}
