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

import org.joda.time.DateTime.now
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.apigateway.connector.impl.DelegatedAuthorityConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.InvalidCredentials
import uk.gov.hmrc.apigateway.model.{ApiRequest, Authority, ProxyRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AuthorityService @Inject()(delegatedAuthorityConnector: DelegatedAuthorityConnector) {

  def findAuthority(request: Request[AnyContent], proxyRequest: ProxyRequest, apiRequest: ApiRequest): Future[Authority] = {

    def getDelegatedAuthority(proxyRequest: ProxyRequest): Future[Authority] = {
      proxyRequest.accessToken(request, apiRequest).flatMap { accessToken =>
        delegatedAuthorityConnector.getByAccessToken(accessToken)
      }
    }

    def hasExpired(authority: Authority) = authority.delegatedAuthority.token.expiresAt.isBefore(now)

    def validateAuthority(authority: Authority, accessToken: String) = {
      if (hasExpired(authority))
        throw InvalidCredentials(request, apiRequest)
      else
        authority
    }

    for {
      accessToken <- proxyRequest.accessToken(request, apiRequest)
      authority <- getDelegatedAuthority(proxyRequest)
    } yield validateAuthority(authority, accessToken)

  }

}
