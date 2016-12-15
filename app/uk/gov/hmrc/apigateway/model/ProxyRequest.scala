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

package uk.gov.hmrc.apigateway.model

import play.api.mvc.RequestHeader

case class ProxyRequest
(httpMethod: String,
 path: String,
 headers: Map[String, String] = Map.empty,
 httpVersion: String = "HTTP/1.1") {

  def getHeader(name: String): Option[String] = headers.get(name)

}

object ProxyRequest {

  def apply(requestHeader: RequestHeader): ProxyRequest =
    ProxyRequest(requestHeader.method, requestHeader.uri.stripPrefix("/api-gateway"),
      requestHeader.headers.headers.toMap, requestHeader.version)

}