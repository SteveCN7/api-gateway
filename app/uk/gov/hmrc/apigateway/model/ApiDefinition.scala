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

package uk.gov.hmrc.apigateway.model

import java.util.UUID

import uk.gov.hmrc.apigateway.model.AuthType.{AuthType, NONE}

object AuthType extends Enumeration {
  type AuthType = Value
  val NONE, USER, APPLICATION = Value

  def authType(string: String) = AuthType.values.find(_.toString == string)
}

case class ApiDefinition(context: String, serviceBaseUrl: String, versions: Seq[ApiVersion])

case class ApiVersion(version: String, endpoints: Seq[ApiEndpoint])

case class ApiEndpoint(uriPattern: String,
                       method: String,
                       authType: AuthType,
                       scope: Option[String] = None,
                       queryParameters: Option[Seq[Parameter]] = None)

case class Parameter(name: String, required: Boolean = false)

case class ApiRequest(requestId: Option[String] = Some(UUID.randomUUID().toString),
                      timeInNanos: Option[Long] = None,
                      timeInMillis: Option[Long] = None,
                      apiIdentifier: ApiIdentifier,
                      authType: AuthType = NONE,
                      apiEndpoint: String,
                      scope: Option[String] = None,
                      userOid: Option[String] = None,
                      clientId: Option[String] = None,
                      bearerToken: Option[String] = None)
