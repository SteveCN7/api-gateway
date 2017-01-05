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
import uk.gov.hmrc.apigateway.exception.GatewayError.{InvalidCredentials, NotFound}
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.model.ProxyRequest
import uk.gov.hmrc.apigateway.service.{AuthorityService, ScopeValidator}
import uk.gov.hmrc.apigateway.util.HttpHeaders._

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

/**
  * Filter for inspecting requests for user restricted endpoints and
  * evaluating their eligibility to be proxied to downstream services.
  */
@Singleton
class UserRestrictedEndpointFilter @Inject()
(authorityService: AuthorityService, scopeValidator: ScopeValidator)
(implicit override val mat: Materializer, executionContext: ExecutionContext) extends ApiGatewayFilter {

  private def getAuthority(proxyRequest: ProxyRequest) = {
    authorityService.findAuthority(proxyRequest) recover {
      case error: NotFound => throw InvalidCredentials()
    }
  }

  override def filter(requestHeader: RequestHeader, proxyRequest: ProxyRequest): Future[RequestHeader] =
    requestHeader.tags.get(X_API_GATEWAY_AUTH_TYPE) flatMap authType match {

      case Some(USER) =>
        for {
          authority <- getAuthority(proxyRequest)
          delegatedAuthority = authority.delegatedAuthority
          _ <- scopeValidator.validate(delegatedAuthority, requestHeader.tags.get(X_API_GATEWAY_SCOPE))
        } yield requestHeader
          .withTag(X_APPLICATION_ID, delegatedAuthority.clientId)
          .withTag(AUTHORIZATION, s"Bearer ${delegatedAuthority.authBearerToken}")

      case _ => successful(requestHeader)
    }

}
