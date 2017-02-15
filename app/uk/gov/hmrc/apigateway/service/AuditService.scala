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

import java.util
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import com.google.common.base.Charsets
import org.joda.time.DateTime
import play.api.Configuration
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.apigateway.connector.impl.MicroserviceAuditConnector
import uk.gov.hmrc.apigateway.model.ApiRequest
import uk.gov.hmrc.apigateway.model.AuthType._
import uk.gov.hmrc.apigateway.util.PlayRequestUtils.bodyOf
import uk.gov.hmrc.play.audit.model.{DataCall, MergedDataEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AuditService @Inject()(val configuration: Configuration, val auditConnector: MicroserviceAuditConnector, implicit val mat: Materializer) {

  val auditBodySizeLimit: Int = configuration.getInt("auditBodySizeLimit").getOrElse(99000)

  def auditSuccessfulRequest(request: Request[AnyContent], apiRequest: ApiRequest, response: Result, responseTimestamp: DateTime = DateTime.now()) = {
    val authorisationType = apiRequest.authType match {
      case USER => "user-restricted"
      case APPLICATION => "application-restricted"
      case _ => "open"
    }

    def getRequestTimeInMillis = apiRequest.timeInNanos match {
      case Some(nanos) => TimeUnit.NANOSECONDS.toMillis(nanos)
      case _ => DateTime.now().getMillis
    }

    def successfulRequestEvent(responseBody: String) = {
      MergedDataEvent("api-gateway", "APIGatewayRequestCompleted",
        request = DataCall(
          tags = Map(
            "X-Request-ID" -> request.headers.get("X-Request-ID").getOrElse(""),
            "path" -> request.path.stripPrefix("/api-gateway"),
            "transactionName" -> "Request has been completed via the API Gateway",
            "clientIP" -> request.remoteAddress,
            "clientPort" -> "443",
            "type" -> "Audit")
          ,
          detail = Map(
            "method" -> request.method,
            "authorisationType" -> authorisationType,
            "requestBody" -> truncate(bodyOf(request).getOrElse("-")),
            "apiContext" -> apiRequest.apiIdentifier.context,
            "apiVersion" -> apiRequest.apiIdentifier.version) ++
            addTag("Authorization", apiRequest.bearerToken) ++
            addTag("userOID", apiRequest.userOid) ++
            addTag("applicationProductionClientId", apiRequest.clientId),
          generatedAt = new DateTime(getRequestTimeInMillis)),
        response = DataCall(
          tags = Map(),
          detail = Map(
            "statusCode" -> response.header.status.toString,
            "responseMessage" -> truncate(responseBody)),
          generatedAt = responseTimestamp))
    }

    for {
      responseBody <- bodyOfResponse(response)
      auditResult <- auditConnector.sendMergedEvent(successfulRequestEvent(responseBody))
    } yield auditResult
  }

  private def bodyOfResponse(result: Result)(implicit mat: Materializer): Future[String] = {
    result.body.consumeData map (_.decodeString(Charsets.UTF_8))
  }

  private def addTag(keyName: String, value: Option[String]): Option[(String, String)] = {
    value map ( keyName -> _ )
  }

  private def truncate(data: String): String = {
    if (data.getBytes.length > auditBodySizeLimit)
      new String(util.Arrays.copyOf(data.getBytes, auditBodySizeLimit))
    else
      data
  }
}
