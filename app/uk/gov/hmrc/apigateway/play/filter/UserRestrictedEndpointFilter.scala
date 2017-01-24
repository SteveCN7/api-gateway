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
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.service._
import uk.gov.hmrc.apigateway.util.HttpHeaders._

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

/**
  * Filter for inspecting requests for user restricted endpoints and
  * evaluating their eligibility to be proxied to downstream services.
  */
@Singleton
class UserRestrictedEndpointFilter @Inject()
(authorityService: AuthorityService, applicationService: ApplicationService, scopeValidator: ScopeValidator)
(implicit override val mat: Materializer, executionContext: ExecutionContext) extends ApiGatewayFilter {

  private def getApplicationByServerToken(proxyRequest: ProxyRequest): Future[Application] =
    applicationService.getByServerToken(accessToken(proxyRequest)) recover {
      case e: NotFound => throw InvalidCredentials()
    }

  private def getAuthority(proxyRequest: ProxyRequest): Future[Authority] =
    authorityService.findAuthority(proxyRequest) recoverWith {
      case e: NotFound => getApplicationByServerToken(proxyRequest).map(_ => throw IncorrectAccessTokenType())
    }

  private def getApplicationByClientId(clientId: String): Future[Application] =
    applicationService.getByClientId(clientId) recover {
      case e: NotFound =>
        Logger.error(s"No application found for the client id: $clientId")
        throw ServerError()
    }

  private def validateRequestAndSwapToken(requestHeader: RequestHeader, proxyRequest: ProxyRequest): Future[RequestHeader] = {
    for {
      authority <- getAuthority(proxyRequest)
      delegatedAuthority = authority.delegatedAuthority
      application <- getApplicationByClientId(delegatedAuthority.clientId)
      _ <- applicationService.validateApplicationIsSubscribedToApi(application.id.toString,
        requestHeader.tags(X_API_GATEWAY_API_CONTEXT), requestHeader.tags(X_API_GATEWAY_API_VERSION))
      _ <- scopeValidator.validate(delegatedAuthority, requestHeader.tags.get(X_API_GATEWAY_SCOPE))
    } yield requestHeader
      .withTag(AUTHORIZATION, s"Bearer ${delegatedAuthority.authBearerToken}")
      .withTag(X_API_GATEWAY_AUTHORIZATION_TOKEN, proxyRequest.accessToken.getOrElse(""))
      .withTag(X_API_GATEWAY_CLIENT_ID, delegatedAuthority.clientId)
  }

  override def filter(requestHeader: RequestHeader, proxyRequest: ProxyRequest): Future[RequestHeader] = {
    requestHeader.tags.get(X_API_GATEWAY_AUTH_TYPE) flatMap authType match {
      case Some(USER) => validateRequestAndSwapToken(requestHeader, proxyRequest)
      case _ => successful(requestHeader)
    }

  }

}