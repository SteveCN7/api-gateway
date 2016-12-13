package uk.gov.hmrc.model

import play.api.mvc.RequestHeader

case class ProxyRequest
(httpMethod: String,
 path: String,
 headers: Map[String, String] = Map.empty,
 httpVersion: String = "HTTP/1.1") {

  def getHeader(name: String): Option[String] = headers.get(name)

}

object ProxyRequest {

  def apply(requestHeader: RequestHeader): ProxyRequest =
    ProxyRequest(requestHeader.method, requestHeader.uri.stripPrefix("/api-gateway"),
      requestHeader.headers.headers.toMap, requestHeader.version)

}
