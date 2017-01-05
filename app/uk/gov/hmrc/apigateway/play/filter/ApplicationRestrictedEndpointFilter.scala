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
import uk.gov.hmrc.apigateway.exception.GatewayError.{NotFound => _, InvalidCredentials, MissingCredentials}
import uk.gov.hmrc.apigateway.model.AuthType.{authType, APPLICATION}
import uk.gov.hmrc.apigateway.model.{Application, ProxyRequest}
import uk.gov.hmrc.apigateway.service.{ApplicationService, AuthorityService}
import uk.gov.hmrc.apigateway.util.HttpHeaders._

import scala.concurrent.Future._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Filter for inspecting requests for application restricted endpoints and
  * evaluating their eligibility to be proxied to a downstream services.
  */
@Singleton
class ApplicationRestrictedEndpointFilter @Inject()
(authorityService: AuthorityService, applicationService: ApplicationService)
(implicit override val mat: Materializer, executionContext: ExecutionContext) extends ApiGatewayFilter {

  def getAppByAuthority(proxyRequest: ProxyRequest): Future[Application] = {
    for {
      authority <- authorityService.findAuthority(proxyRequest)
      app <- applicationService.getByClientId(authority.delegatedAuthority.clientId)
    } yield app
  }

  def getApplication(serverToken: String, proxyRequest: ProxyRequest): Future[Application] = {
    getAppByAuthority(proxyRequest).recoverWith {
      case InvalidCredentials() => Future.failed(InvalidCredentials())
      case _ => applicationService.getByServerToken(serverToken)
    }
  }

  override def filter(requestHeader: RequestHeader, proxyRequest: ProxyRequest): Future[RequestHeader] =
    requestHeader.tags.get(X_API_GATEWAY_AUTH_TYPE) flatMap authType match {
      case Some(APPLICATION) =>
        proxyRequest.accessToken match {
          case Some(serverToken) =>
            getApplication(serverToken, proxyRequest).map(app => requestHeader.withTag(X_APPLICATION_ID, app.id.toString))
          case _ => throw MissingCredentials()
        }
      case _ => successful(requestHeader)
    }
}
