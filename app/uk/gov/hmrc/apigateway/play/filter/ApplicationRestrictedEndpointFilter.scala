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

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import play.api.mvc._
import uk.gov.hmrc.apigateway.exception.GatewayError.{MissingCredentials, NotFound => _}
import uk.gov.hmrc.apigateway.model.AuthType.{authType, APPLICATION}
import uk.gov.hmrc.apigateway.model.ProxyRequest
import uk.gov.hmrc.apigateway.service.AuthorityService
import uk.gov.hmrc.apigateway.util.HttpHeaders._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

/**
  * Filter for inspecting requests for application restricted endpoints and
  * evaluating their eligibility to be proxied to a downstream services.
  */
@Singleton
class ApplicationRestrictedEndpointFilter @Inject()
(authorityService: AuthorityService)
(implicit override val mat: Materializer, executionContext: ExecutionContext) extends ApiGatewayFilter {

  override def filter(requestHeader: RequestHeader, proxyRequest: ProxyRequest): Future[RequestHeader] =
    requestHeader.tags.get(X_API_GATEWAY_AUTH_TYPE) flatMap authType match {
      case Some(APPLICATION) =>
        authorityService.findAuthority(proxyRequest) map { authority =>
          requestHeader.withTag(X_APPLICATION_CLIENT_ID, authority.delegatedAuthority.clientId)
        } recover {
          case _ => requestHeader.headers.get(AUTHORIZATION) match {
            case Some(bearerToken) =>
              //TODO Fetch Client ID from third-party-application
              requestHeader
            case _ => throw MissingCredentials()
          }
        }
      case _ => successful(requestHeader)
    }
}
