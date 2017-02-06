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

import play.api.Configuration
import uk.gov.hmrc.apigateway.connector.impl.ThirdPartyApplicationConnector
import uk.gov.hmrc.apigateway.exception.GatewayError._
import uk.gov.hmrc.apigateway.model.RateLimitTier.{SILVER, GOLD}
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.repository.RateLimitRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

@Singleton
class ApplicationService @Inject()(applicationConnector: ThirdPartyApplicationConnector,
                                   rateLimitRepository: RateLimitRepository,
                                   configuration: Configuration) {

  def getByServerToken(serverToken: String): Future[Application] = {
    applicationConnector.getApplicationByServerToken(serverToken)
  }

  def getByClientId(clientId: String): Future[Application] = {
    applicationConnector.getApplicationByClientId(clientId)
  }

  def validateSubscriptionAndRateLimit(application: Application, requestedApi: ApiIdentifier): Future[Unit] = {
    val validateSubscription = validateApplicationIsSubscribedToApi(application.id.toString, requestedApi)
    val validateRateLimit = validateApplicationRateLimit(application)

    for {
      _ <- validateSubscription
      _ <- validateRateLimit
    } yield ()
  }

  private def validateApplicationIsSubscribedToApi(applicationId: String, requestedApi: ApiIdentifier): Future[Unit] = {

    def subscribed(appSubscriptions: Seq[Api]): Boolean = {
      appSubscriptions.exists { api: Api =>
        api.context == requestedApi.context && api.versions.exists { sub: Subscription =>
          sub.subscribed && sub.version.version == requestedApi.version
        }
      }
    }

    applicationConnector.getSubscriptionsByApplicationId(applicationId) flatMap {
      case appSubscriptions: Seq[Api] if subscribed(appSubscriptions) => successful(())
      case _ => failed(InvalidSubscription())
    }
  }

  private def validateApplicationRateLimit(application: Application): Future[Unit] = {
    rateLimitRepository.validateAndIncrement(application.clientId, rateLimit(application))
  }

  private def rateLimit(application: Application): Int = {
    val rateLimitProperty = application.rateLimitTier match {
      case GOLD => "rateLimit.gold"
      case SILVER => "rateLimit.silver"
      case _ => "rateLimit.bronze"
    }
    configuration.getInt(rateLimitProperty).getOrElse(throw new RuntimeException(s"$rateLimitProperty is not configured"))
  }
}