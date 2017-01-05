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
import uk.gov.hmrc.apigateway.model.Application
import uk.gov.hmrc.apigateway.play.binding.PlayBindings.applicationFormat

import scala.concurrent.Future

@Singleton
class ThirdPartyApplicationConnector @Inject() (wsClient: WSClient, cache: CacheManager)
  extends ServiceConnector(wsClient, cache, "third-party-application") {

  val SERVER_TOKEN_HEADER = "X-server-token"

  def getByServerToken(serverToken: String): Future[Application] = {
    get[Application](s"$serviceName-$serverToken", s"application", Seq((SERVER_TOKEN_HEADER, serverToken)))
  }

  def getByClientId(clientId: String): Future[Application] = {
    get[Application](s"$serviceName-$clientId", s"application?clientId=$clientId")
  }
}
