package uk.gov.hmrc.connector

import play.api.Logger
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Format
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class AbstractConnector(wsClient: WSClient) {

  def get[T](url: String)(implicit format: Format[T]): Future[T] = {
    wsClient.url(url).get() map {
      case wsResponse if wsResponse.status >= OK && wsResponse.status < 300 =>
        Logger.debug(s"GET $url ${wsResponse.status}")
        wsResponse.json.as[T]

      case wsResponse if wsResponse.status == NOT_FOUND =>
        Logger.debug(s"GET $url ${wsResponse.status}")
        throw new RuntimeException("Resource cannot be found")
    }
  }

}
