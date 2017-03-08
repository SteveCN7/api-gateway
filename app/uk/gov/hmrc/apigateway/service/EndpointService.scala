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

import org.joda.time.DateTimeUtils
import play.api.Logger
import uk.gov.hmrc.apigateway.connector.impl.ApiDefinitionConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.{MatchingResourceNotFound, NotFound}
import uk.gov.hmrc.apigateway.model._
import uk.gov.hmrc.apigateway.service.EndpointService._
import uk.gov.hmrc.apigateway.util.HttpHeaders.ACCEPT
import uk.gov.hmrc.apigateway.util.ProxyRequestUtils.{parseVersion, validateContext}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.{failed, successful}

@Singleton
class EndpointService @Inject()(apiDefinitionConnector: ApiDefinitionConnector) {

  def apiRequest(proxyRequest: ProxyRequest) = {
    for {
      context <- validateContext(proxyRequest)
      version <- parseVersion(proxyRequest)
      apiDefinition <- apiDefinitionConnector.getByContext(context)
      apiEndpoint <- findEndpoint(proxyRequest, context, version, apiDefinition)
    } yield createAndLogApiRequest(proxyRequest, context, version, apiDefinition, apiEndpoint)
  }
}

object EndpointService {

  private def createAndLogApiRequest(proxyRequest: ProxyRequest, context: String, version: String, apiDefinition: ApiDefinition, apiEndpoint: ApiEndpoint) = {
    val apiReq = ApiRequest(
      timeInNanos = Some(System.nanoTime()),
      timeInMillis = Some(DateTimeUtils.currentTimeMillis()),
      apiIdentifier = ApiIdentifier(context, version),
      authType = apiEndpoint.authType,
      apiEndpoint = s"${apiDefinition.serviceBaseUrl}/${proxyRequest.path}",
      scope = apiEndpoint.scope)

    Logger.debug(s"successful api request match for [${stringify(proxyRequest)}] to [$apiReq]")

    apiReq
  }

  private def findEndpoint(proxyRequest: ProxyRequest, requestContext: String, requestVersion: String, apiDefinition: ApiDefinition) = {

    def filterEndpoint(apiEndpoint: ApiEndpoint): Boolean = {
      apiEndpoint.method == proxyRequest.httpMethod &&
        pathMatchesPattern(apiEndpoint.uriPattern, proxyRequest.rawPath) &&
        queryParametersMatch(proxyRequest.queryParameters, apiEndpoint.queryParameters)
    }

    val apiVersion = apiDefinition.versions.find(_.version == requestVersion)
    val apiEndpoint = apiVersion.flatMap(_.endpoints.find(filterEndpoint))

    (apiVersion, apiEndpoint) match {
      case (None, _) => failed(NotFound())
      case (_, None) => failed(MatchingResourceNotFound())
      case (_, Some(endpoint)) => successful(endpoint)
    }
  }

  private def pathMatchesPattern(uriPattern: String, path: String): Boolean = {
    val pattern = parseUriPattern(uriPattern)
    val pathParts = parsePathParts(path).drop(1)

    pattern.length == pathParts.length && pattern.zip(pathParts).forall {
      case (Variable, _) => true
      case (PathPart(requiredPart), providedPart) => requiredPart == providedPart
    }
  }

  private def queryParametersMatch(queryParameters: Map[String, Seq[String]],
                                   endpointQueryParameters: Option[Seq[Parameter]] = None) = {
    endpointQueryParameters match {
      case Some(configuredParams) if configuredParams.exists(_.required) =>
        configuredParams.flatMap(cp => queryParameters.get(cp.name)).flatten.nonEmpty
      case None => true
    }
  }

  private def parsePathParts(value: String) =
    value.stripPrefix("/").split("/")

  private def parseUriPattern(value: String) =
    parsePathParts(value).map {
      case part if part.startsWith("{") && part.endsWith("}") => Variable
      case part => PathPart(part)
    }

  private def stringify(proxyRequest: ProxyRequest): String =
    s"${proxyRequest.httpMethod} ${proxyRequest.path} ${proxyRequest.getHeader(ACCEPT).getOrElse("")}"

  sealed trait UriPatternPart

  case object Variable extends UriPatternPart

  case class PathPart(part: String) extends UriPatternPart

}
