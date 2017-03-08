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
import uk.gov.hmrc.apigateway.config.AppContext
import uk.gov.hmrc.apigateway.connector.impl.ThirdPartyApplicationConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.{NotFound, ServerError}
import uk.gov.hmrc.apigateway.model.RateLimitTier.{GOLD, SILVER}
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.repository.RateLimitRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ApplicationService @Inject()(applicationConnector: ThirdPartyApplicationConnector,
                                   rateLimitRepository: RateLimitRepository,
                                   appContext: AppContext) {

  def getByServerToken(serverToken: String): Future[Application] = {
    applicationConnector.getApplicationByServerToken(serverToken)
  }

  def getByClientId(clientId: String): Future[Application] = {
    applicationConnector.getApplicationByClientId(clientId) recover {
      case e: NotFound =>
        Logger.error(s"No application found for the client id: $clientId")
        throw ServerError()
    }
  }

  def validateSubscriptionAndRateLimit(application: Application, requestedApi: ApiIdentifier): Future[Unit] = {
    val validateSubscription = applicationConnector.validateSubscription(application.id.toString, requestedApi)
    val validateRateLimit = validateApplicationRateLimit(application)

    for {
      _ <- validateSubscription
      _ <- validateRateLimit
    } yield ()
  }

  private def validateApplicationRateLimit(application: Application): Future[Unit] = {
    rateLimitRepository.validateAndIncrement(application.clientId, rateLimit(application))
  }

  private def rateLimit(application: Application): Int = {
    application.rateLimitTier match {
      case GOLD => appContext.rateLimitGold
      case SILVER => appContext.rateLimitSilver
      case _ => appContext.rateLimitBronze
    }
  }
}