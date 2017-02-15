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
import play.api.mvc._
import uk.gov.hmrc.apigateway.connector.AbstractConnector
import uk.gov.hmrc.apigateway.model.ApiRequest
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.apigateway.util.PlayRequestUtils.bodyOf

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ProxyConnector @Inject()(wsClient: WSClient) extends AbstractConnector(wsClient: WSClient) {

  def proxy(request: Request[AnyContent], apiRequest: ApiRequest): Future[Result] = {

    val headers = replaceHeaders(request.headers)(
      (HOST, None),
      (AUTHORIZATION, apiRequest.bearerToken),
      (X_CLIENT_AUTHORIZATION_TOKEN, apiRequest.bearerToken.map(_.stripPrefix("Bearer "))),
      (X_CLIENT_ID, apiRequest.clientId),
      (X_REQUEST_TIMESTAMP, apiRequest.timeInNanos.map(_.toString)))

    wsClient.url(apiRequest.apiEndpoint)
      .withMethod(request.method)
      .withHeaders(headers.toSimpleMap.toSeq: _*)
      .withBody(bodyOf(request).getOrElse(""))
      .execute.map { wsResponse =>

      val result = toResult(wsResponse)

      Logger.info(s"request [$request] response [$wsResponse] result [$result]")

      result
    }
  }

  private def replaceHeaders(headers: Headers)(updatedHeaders: (String, Option[String])*): Headers = {
    updatedHeaders.headOption match {
      case Some((headerName, Some(headerValue))) => replaceHeaders(headers.replace(headerName -> headerValue))(updatedHeaders.tail:_*)
      case Some((headerName, None)) => replaceHeaders(headers.remove(headerName))(updatedHeaders.tail:_*)
      case None => headers
    }
  }

  private def toResult(streamedResponse: WSResponse): Result =
    Result(
      ResponseHeader(streamedResponse.status, flattenHeaders(streamedResponse.allHeaders)),
      HttpEntity.Strict(streamedResponse.bodyAsBytes, None)
    )

  private def flattenHeaders(headers: Map[String, Seq[String]]) =
    headers.mapValues(_.mkString(","))

}
