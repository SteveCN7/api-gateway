package uk.gov.hmrc.util

import uk.gov.hmrc.exception.GatewayError.{ContextNotFound, InvalidAcceptHeader}
import uk.gov.hmrc.model.ProxyRequest
import uk.gov.hmrc.util.HttpHeaders.ACCEPT

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import scala.util.matching.Regex

object ProxyRequestUtils {

  private val parseContext = firstGroup("""\/([^\/]*).*""".r)
  private val parseVersion = firstGroup("""application\/vnd\.hmrc\.(.*)\+.*""".r)

  def validateContext[T](proxyRequest: ProxyRequest): Future[String] =
    validateOrElse(parseContext(proxyRequest.path), ContextNotFound())

  def validateVersion[T](proxyRequest: ProxyRequest): Future[String] = {
    val acceptHeader: String = proxyRequest.getHeader(ACCEPT).getOrElse("")
    validateOrElse(parseVersion(acceptHeader), InvalidAcceptHeader())
  }

  private def validateOrElse(maybeString: Option[String], throwable: Throwable): Future[String] =
    maybeString map successful getOrElse failed(throwable)

  private def firstGroup(regex: Regex) = { value: String =>
    regex.unapplySeq(value) flatMap (_.headOption)
  }

}
