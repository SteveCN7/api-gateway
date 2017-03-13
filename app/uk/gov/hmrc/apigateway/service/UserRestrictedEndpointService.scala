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

package uk.gov.hmrc.apigateway.service

import javax.inject.{Inject, Singleton}

import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UserRestrictedEndpointService @Inject()(authorityService: AuthorityService,
                                              applicationService: ApplicationService,
                                              scopeValidator: ScopeValidator) {

  def routeRequest(request: Request[AnyContent], proxyRequest: ProxyRequest, apiRequest: ApiRequest) = {

    def getApplication(accessToken: String) = {
      applicationService.getByServerToken(accessToken) recover {
        case e: NotFound =>
          throw InvalidCredentials(request, apiRequest.copy(bearerToken = Some(s"Bearer $accessToken")))
      }
    }

    def getApplicationByServerToken = {
      proxyRequest.accessToken(request, apiRequest) flatMap getApplication
    }

    def getAuthority = {
      authorityService.findAuthority(request, proxyRequest, apiRequest) recoverWith {
        case e: NotFound => getApplicationByServerToken.map(_ => throw IncorrectAccessTokenType())
      }
    }

    getAuthority flatMap { authority =>
      val validateScopes: Future[Unit] = scopeValidator.validate(authority.delegatedAuthority, apiRequest.scope)

      for {
        application <- applicationService.getByClientId(authority.delegatedAuthority.clientId)
        _ <- applicationService.validateSubscriptionAndRateLimit(application, apiRequest.apiIdentifier)
        _ <- validateScopes
      } yield apiRequest.copy(
        userOid = authority.delegatedAuthority.user.map(_.userId),
        clientId = Some(authority.delegatedAuthority.clientId),
        bearerToken = Some(s"Bearer ${authority.delegatedAuthority.authBearerToken}"))
    }
  }

}
