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

import javax.inject.{Inject, Singleton}

import com.google.common.net.HttpHeaders._
import play.api.http.HttpVerbs._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.apigateway.connector.impl.ProxyConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.ServiceNotAvailable
import uk.gov.hmrc.apigateway.model.ApiRequest
import uk.gov.hmrc.apigateway.model.AuthType.NONE
import uk.gov.hmrc.apigateway.util.PlayRequestUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ProxyService @Inject()(proxyConnector: ProxyConnector, auditService: AuditService) {

  def proxy(request: Request[AnyContent], apiRequest: ApiRequest)(implicit requestId: String): Future[Result] = {
    val httpMethodsToValidate = Seq(POST, PUT, PATCH)

    def proxyF: Future[Result] = proxyConnector.proxy(request, apiRequest) map { response =>
      if (apiRequest.authType != NONE) {
        auditService.auditSuccessfulRequest(request, apiRequest, response)
      }
      response
    }

    val requiresValidation = httpMethodsToValidate.contains(request.method)
    val contentType = request.headers.get(CONTENT_TYPE).getOrElse("")
    val body = bodyOf(request).getOrElse("")

    (requiresValidation, contentType, body) match {
      case (true, `contentType`, _) if contentType.isEmpty => throw ServiceNotAvailable()
      case (true, _, `body`) if body.isEmpty => throw ServiceNotAvailable()
      case (_, _, _) => proxyF
    }
  }

}
