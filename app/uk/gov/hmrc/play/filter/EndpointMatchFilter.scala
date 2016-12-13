package uk.gov.hmrc.play.filter

import javax.inject.{Inject, Singleton}

import play.api.Logger
import uk.gov.hmrc.connector.impl.ApiDefinitionConnector
import uk.gov.hmrc.exception.GatewayError.MatchingResourceNotFound
import uk.gov.hmrc.model.{ApiDefinition, ApiDefinitionMatch, ApiEndpoint, ProxyRequest}
import uk.gov.hmrc.play.filter.EndpointMatchFilter.{createAndLogApiDefinitionMatch, findEndpoint}
import uk.gov.hmrc.util.HttpHeaders.ACCEPT
import uk.gov.hmrc.util.ProxyRequestUtils.{validateContext, validateVersion}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

@Singleton
class EndpointMatchFilter @Inject()(apiDefinitionConnector: ApiDefinitionConnector) {

  def filter(proxyRequest: ProxyRequest): Future[ApiDefinitionMatch] =
    for {
      requestContext <- validateContext(proxyRequest)
      requestVersion <- validateVersion(proxyRequest)
      apiDefinition <- apiDefinitionConnector.getByContext(requestContext)
      apiEndpoint <- findEndpoint(proxyRequest, requestContext, requestVersion, apiDefinition)
    } yield createAndLogApiDefinitionMatch(proxyRequest, requestContext, apiDefinition, requestVersion, apiEndpoint)

}

object EndpointMatchFilter {

  private def createAndLogApiDefinitionMatch(proxyRequest: ProxyRequest, requestContext: String, apiDefinition: ApiDefinition, requestVersion: String, apiEndpoint: ApiEndpoint): ApiDefinitionMatch = {
    val apiDefinitionMatch = ApiDefinitionMatch(requestContext, apiDefinition.serviceBaseUrl, requestVersion, apiEndpoint.scope)
    Logger.debug(s"successfull endpoint match for [${stringify(proxyRequest)}] to [$apiDefinitionMatch]")
    apiDefinitionMatch
  }

  private def findEndpoint(proxyRequest: ProxyRequest, requestContext: String, requestVersion: String, apiDefinition: ApiDefinition) = {
    (for {
      apiVersion <- apiDefinition.versions.find(_.version == requestVersion)
      apiEndpoint <- apiVersion.endpoints.find { endpoint =>
        endpoint.method == proxyRequest.httpMethod && pathMatchesPattern(endpoint.uriPattern, proxyRequest.path)
      }
    } yield apiEndpoint).map(successful).getOrElse(failed(MatchingResourceNotFound()))
  }

  private def pathMatchesPattern(uriPattern: String, path: String): Boolean = {
    val pattern = parseUriPattern(uriPattern)
    val pathParts = parsePathParts(path).drop(1)

    pattern.length == pathParts.length && pattern.zip(pathParts).forall {
      case (Variable, _) => true
      case (PathPart(requiredPart), providedPart) => requiredPart == providedPart
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
