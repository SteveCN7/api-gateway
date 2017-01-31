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

package uk.gov.hmrc.apigateway.play.filter

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import org.joda.time.DateTime
import play.api.mvc._
import uk.gov.hmrc.apigateway.exception.GatewayError.{NotFound => _}
import uk.gov.hmrc.apigateway.model.ProxyRequest
import uk.gov.hmrc.apigateway.service.EndpointService
import uk.gov.hmrc.apigateway.util.HttpHeaders.AUTHORIZATION
import uk.gov.hmrc.apigateway.util.RequestTags._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Filter for inspecting requests for generic (open and/or restricted) endpoints
  * and evaluating their eligibility to be proxied to downstream services.
  */
@Singleton
class GenericEndpointFilter @Inject()
(endpointService: EndpointService)
(implicit override val mat: Materializer, executionContext: ExecutionContext) extends ApiGatewayFilter {

  override def filter(requestHeader: RequestHeader, proxyRequest: ProxyRequest): Future[RequestHeader] = {
    for {
      apiDefinitionMatch <- endpointService.findApiDefinition(proxyRequest)
    // TODO implement global rate limit filter???
    } yield {
      requestHeader.copy(tags = requestHeader.tags +
          (API_CONTEXT -> apiDefinitionMatch.context) +
          (API_VERSION -> apiDefinitionMatch.apiVersion) +
          (API_ENDPOINT -> s"${apiDefinitionMatch.serviceBaseUrl}/${proxyRequest.path}") +
          (AUTH_TYPE -> apiDefinitionMatch.authType.toString) +
          (REQUEST_TIMESTAMP_MILLIS -> DateTime.now().getMillis.toString) +
          (REQUEST_TIMESTAMP_NANO -> System.nanoTime().toString) ++
          apiDefinitionMatch.scope.map(s => (API_SCOPE, s)) ++
          requestHeader.headers.get(AUTHORIZATION).map(a => (OAUTH_AUTHORIZATION, a))
        )
      }
    }
}
