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

import uk.gov.hmrc.apigateway.model.AuthType.AuthType

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

case class ApiDefinitionMatch(context: String, serviceBaseUrl: String, apiVersion: String, authType: AuthType, scope: Option[String])
