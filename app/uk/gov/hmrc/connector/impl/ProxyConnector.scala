package uk.gov.hmrc.connector.impl

import javax.inject.{Inject, Singleton}

import play.Logger
import play.api.http.HttpEntity
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{AnyContent, Request, ResponseHeader, Result}
import uk.gov.hmrc.connector.AbstractConnector
import uk.gov.hmrc.util.HttpHeaders._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ProxyConnector @Inject()(wsClient: WSClient) extends AbstractConnector(wsClient: WSClient) {

  def proxy(request: Request[AnyContent], destinationUrl: String): Future[Result] =
    wsClient.url(destinationUrl)
      .withMethod(request.method)
      .withHeaders((ACCEPT, request.tags(ACCEPT)))
      .withBody(request.body.toString) // TODO do this properly
      .execute.map { wsResponse =>
      val result = toResult(wsResponse)
      Logger.info(s"request [$request] response [$wsResponse] result [$result]")
      result
    }

  private def toResult(streamedResponse: WSResponse): Result =
    Result(
      ResponseHeader(streamedResponse.status, flattenHeaders(streamedResponse.allHeaders)),
      HttpEntity.Strict(streamedResponse.bodyAsBytes, None)
    )

  // this is just depressing
  private def flattenHeaders(headers: Map[String, Seq[String]]) = (for {
    (key, vals) <- headers.toSeq
    value <- vals
  } yield (key, value)).toMap

}
