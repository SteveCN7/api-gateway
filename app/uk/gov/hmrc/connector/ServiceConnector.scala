package uk.gov.hmrc.connector

import play.api.libs.json.Format
import play.api.libs.ws.WSClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

abstract class ServiceConnector(wsClient: WSClient, val serviceName: String)
  extends AbstractConnector(wsClient) with ServicesConfig {

  private lazy val serviceBaseUrl = baseUrl(serviceName)

  override def get[T](urlPath: String)(implicit format: Format[T]): Future[T] =
    super.get(s"$serviceBaseUrl/$urlPath")

}
