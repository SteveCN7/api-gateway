/*
 * Copyright 2016 HM Revenue & Customs
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

import javax.inject.Singleton

import uk.gov.hmrc.apigateway.exception.GatewayError.InvalidScope
import uk.gov.hmrc.apigateway.model.Authority

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

@Singleton
class ScopeValidator {

  def validate(authority: Authority, maybeScope: Option[String]): Future[Boolean] = maybeScope match {
    case Some(scope) if authority.delegatedAuthority.token.scopes.contains(scope) => successful(true)
    case _ => failed(InvalidScope())
  }
}