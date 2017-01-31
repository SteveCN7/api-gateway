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
import uk.gov.hmrc.apigateway.util.RequestTags._

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
    getAuthority(proxyRequest) flatMap { authority =>

      val validateSubscriptions: Future[Unit] = for {
        application <- getApplicationByClientId(authority.delegatedAuthority.clientId)
        _ <- applicationService.validateApplicationIsSubscribedToApi(application.id.toString,
          requestHeader.tags(API_CONTEXT), requestHeader.tags(API_VERSION))
      } yield ()

      val validateScopes: Future[Unit] = scopeValidator.validate(authority.delegatedAuthority, requestHeader.tags.get(API_SCOPE))

      for {
        _ <- validateSubscriptions
        _ <- validateScopes
      } yield requestHeader
        .withTag(AUTH_AUTHORIZATION, s"Bearer ${authority.delegatedAuthority.authBearerToken}")
        .withTag(USER_OID, authority.delegatedAuthority.user.map(_.userId).getOrElse(""))
        .withTag(CLIENT_ID, authority.delegatedAuthority.clientId)
    }
  }

  override def filter(requestHeader: RequestHeader, proxyRequest: ProxyRequest): Future[RequestHeader] = {
    requestHeader.tags.get(AUTH_TYPE) flatMap authType match {
      case Some(USER) => validateRequestAndSwapToken(requestHeader, proxyRequest)
      case _ => successful(requestHeader)
    }

  }

}