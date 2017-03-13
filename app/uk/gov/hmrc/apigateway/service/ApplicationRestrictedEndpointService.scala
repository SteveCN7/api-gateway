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

import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ApplicationRestrictedEndpointService @Inject()(authorityService: AuthorityService,
                                                     applicationService: ApplicationService) {

  def routeRequest(request: Request[AnyContent], proxyRequest: ProxyRequest, apiRequest: ApiRequest) = {

    def getAuthority(accessToken: String) = {
      authorityService.findAuthority(request, proxyRequest, apiRequest) recover {
        case e: NotFound =>
          Logger.debug("No authority found for the access token")
          throw InvalidCredentials(request, apiRequest.copy(bearerToken = Some(s"Bearer $accessToken")))
      }
    }

    def getApplicationByAuthority(accessToken: String) = {
      for {
        authority <- getAuthority(accessToken)
        app <- applicationService.getByClientId(authority.delegatedAuthority.clientId)
      } yield app
    }

    def getApplication(accessToken: String) = {
      applicationService.getByServerToken(accessToken) recoverWith {
        case e: NotFound => getApplicationByAuthority(accessToken)
      }
    }

    for {
      accessToken <- proxyRequest.accessToken(request, apiRequest)
      app <- getApplication(accessToken)
      _ <- applicationService.validateSubscriptionAndRateLimit(app, apiRequest.apiIdentifier)
    } yield apiRequest.copy(clientId = Some(app.clientId))
  }

}
