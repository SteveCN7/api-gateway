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

object HttpHeaders {
  val ACCEPT = "Accept"
  val AUTHORIZATION = "Authorization"
  val X_CLIENT_AUTHORIZATION_TOKEN = "X-Client-Authorization-Token"
  val X_CLIENT_ID = "X-Client-ID"
  val X_REQUEST_TIMESTAMP = "X-Request-Timestamp"
  val X_SERVER_TOKEN = "X-server-token"
}

object RequestTags {
  val API_CONTEXT = "API_CONTEXT"
  val API_ENDPOINT = "API_ENDPOINT"
  val API_SCOPE = "API_SCOPE"
  val API_VERSION = "API_VERSION"
  val AUTH_TYPE = "AUTH_TYPE"
  val CLIENT_ID = "CLIENT_ID"
  val OAUTH_AUTHORIZATION = "OAUTH_AUTHORIZATION"
  val AUTH_AUTHORIZATION = "AUTH_AUTHORIZATION"
  val REQUEST_TIMESTAMP_MILLIS = "REQUEST_TIMESTAMP_MILLIS"
  val REQUEST_TIMESTAMP_NANO = "REQUEST_TIMESTAMP_NANO"
  val USER_OID = "USER_OID"
}
