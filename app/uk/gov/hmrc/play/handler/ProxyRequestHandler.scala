package uk.gov.hmrc.play.handler

import javax.inject.{Singleton, Inject}

import play.api.http.{DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router
import uk.gov.hmrc.controller.ProxyController

@Singleton
class ProxyRequestHandler @Inject()
(errorHandler: HttpErrorHandler,
 configuration: HttpConfiguration,
 filters: HttpFilters,
 proxyRoutes: Router,
 proxyController: ProxyController)
  extends DefaultHttpRequestHandler(proxyRoutes, errorHandler, configuration, filters) {

  override def routeRequest(requestHeader: RequestHeader): Option[Handler] = Some(proxyController.proxy)

}
