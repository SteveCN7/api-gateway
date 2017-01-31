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

import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.apigateway.connector.impl.ProxyConnector
import uk.gov.hmrc.apigateway.util.RequestTags.API_ENDPOINT

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ProxyService @Inject()(proxyConnector: ProxyConnector, auditService: AuditService) {

  def proxy(request: Request[AnyContent]): Future[Result] = {
    proxyConnector.proxy(request, request.tags(API_ENDPOINT)) map { response =>
      auditService.auditSuccessfulRequest(request, response)
      response
    }
  }

}
