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

package uk.gov.hmrc.apigateway.connector.impl

import javax.inject.{Inject, Singleton}

import play.api.libs.ws.WSClient
import uk.gov.hmrc.apigateway.cache.CacheManager
import uk.gov.hmrc.apigateway.connector.ServiceConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.{NotFound, InvalidSubscription}
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._
import uk.gov.hmrc.apigateway.util.HttpHeaders.X_SERVER_TOKEN

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future._

@Singleton
class ThirdPartyApplicationConnector @Inject() (wsClient: WSClient, cache: CacheManager)
  extends ServiceConnector(wsClient, cache, serviceName = "third-party-application") {

  def getApplicationByServerToken(serverToken: String): Future[Application] =
    get[Application](
      key = s"$serviceName-$serverToken",
      urlPath = "application",
      headers = Seq(X_SERVER_TOKEN -> serverToken)
    )

  def getApplicationByClientId(clientId: String): Future[Application] =
    get[Application](
      key = s"$serviceName-$clientId",
      urlPath = s"application?clientId=$clientId"
    )

  def validateSubscription(applicationId: String, apiIdentifier: ApiIdentifier): Future[Unit] = {
    get[ApiIdentifier](
      key = s"$serviceName-$applicationId-${apiIdentifier.context}-${apiIdentifier.version}",
      urlPath = s"application/$applicationId/subscription/${apiIdentifier.context}/${apiIdentifier.version}"
    ) map { _ => ()} recoverWith {
      case NotFound() => failed(InvalidSubscription())
    }
  }
}
