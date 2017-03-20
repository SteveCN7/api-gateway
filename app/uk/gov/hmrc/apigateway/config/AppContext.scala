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

package uk.gov.hmrc.apigateway.config

import javax.inject.{Inject, Singleton}

import play.api.Configuration

@Singleton
class AppContext @Inject()(val configuration: Configuration) {

  lazy val auditBodySizeLimit: Int = {
    val propName = "auditBodySizeLimit"
    configuration.getInt(propName).getOrElse(99000)
  }

  lazy val requestTimeoutInMilliseconds: Int = {
    val propName = "requestTimeoutInMilliseconds"
    configuration.getInt(propName).getOrElse(throw new RuntimeException(s"$propName is not configured"))
  }

  lazy val rateLimitGold: Int = {
    val propName = "rateLimit.gold"
    configuration.getInt(propName).getOrElse(throw new RuntimeException(s"$propName is not configured"))
  }
  lazy val rateLimitSilver: Int = {
    val propName = "rateLimit.silver"
    configuration.getInt(propName).getOrElse(throw new RuntimeException(s"$propName is not configured"))
  }
  lazy val rateLimitBronze: Int = {
    val propName = "rateLimit.bronze"
    configuration.getInt(propName).getOrElse(throw new RuntimeException(s"$propName is not configured"))
  }

}
