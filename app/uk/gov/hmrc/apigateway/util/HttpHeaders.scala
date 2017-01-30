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

package uk.gov.hmrc.apigateway.util

import play.api.http.HeaderNames

object HttpHeaders extends HeaderNames {

  val X_API_GATEWAY_ENDPOINT = "x-api-gateway-proxy-endpoint"
  val X_API_GATEWAY_SCOPE = "x-api-gateway-scope"
  val X_API_GATEWAY_AUTH_TYPE = "x-api-gateway-auth-type"
  val X_API_GATEWAY_API_CONTEXT = "x-api-gateway-api-context"
  val X_API_GATEWAY_API_VERSION = "x-api-gateway-api-version"
  val X_API_GATEWAY_SERVER_TOKEN = "X-server-token"
  val X_API_GATEWAY_CLIENT_ID = "X-Client-ID"
  val X_API_GATEWAY_AUTHORIZATION_TOKEN = "X-Client-Authorization-Token"
  val X_API_GATEWAY_REQUEST_TIMESTAMP = "X-Request-Timestamp"

}
