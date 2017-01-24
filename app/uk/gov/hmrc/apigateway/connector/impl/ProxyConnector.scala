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

import play.Logger
import play.api.http.HttpEntity
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{AnyContent, Request, ResponseHeader, Result}
import uk.gov.hmrc.apigateway.connector.AbstractConnector
import uk.gov.hmrc.apigateway.util.HttpHeaders._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ProxyConnector @Inject()(wsClient: WSClient) extends AbstractConnector(wsClient: WSClient) {

  def proxy(request: Request[AnyContent], destinationUrl: String): Future[Result] =
    wsClient.url(destinationUrl)
      .withMethod(request.method)
      .withHeaders(Seq() ++
        header(request, ACCEPT) ++
        header(request, AUTHORIZATION) ++
        header(request, X_API_GATEWAY_CLIENT_ID) ++
        header(request, X_API_GATEWAY_AUTHORIZATION_TOKEN) ++
        header(request, X_API_GATEWAY_REQUEST_TIMESTAMP): _*)
      .withBody(request.body.toString) // TODO this will not work for binary content, we can tackle it when we need it
      .execute.map { wsResponse =>
      val result = toResult(wsResponse)
      Logger.info(s"request [$request] response [$wsResponse] result [$result]")
      result
    }

  private def header(request: Request[AnyContent], headerName: String): Option[(String, String)] = {
    request.tags.get(headerName).map(value => headerName -> value)
  }

  private def toResult(streamedResponse: WSResponse): Result =
    Result(
      ResponseHeader(streamedResponse.status, flattenHeaders(streamedResponse.allHeaders)),
      HttpEntity.Strict(streamedResponse.bodyAsBytes, None)
    )

  private def flattenHeaders(headers: Map[String, Seq[String]]) =
    headers.mapValues(_.mkString(","))

}
