package uk.gov.hmrc.service

import javax.inject.{Inject, Singleton}

import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.connector.impl.ProxyConnector
import uk.gov.hmrc.util.HttpHeaders._

import scala.concurrent.Future

@Singleton
class ProxyService @Inject()(proxyConnector: ProxyConnector) {

  def proxy(request: Request[AnyContent]): Future[Result] =
    proxyConnector.proxy(request, request.tags(X_API_GATEWAY_ENDPOINT))

}
