/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.apigateway.util

import uk.gov.hmrc.apigateway.exception.GatewayError.{InvalidAcceptHeader, NotFound}
import uk.gov.hmrc.apigateway.model.ProxyRequest
import uk.gov.hmrc.apigateway.util.HttpHeaders.ACCEPT

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import scala.util.matching.Regex

object ProxyRequestUtils {

  private val parseContext = firstGroup("""\/([^\/]*).*""".r)
  private val parseVersion = firstGroup("""application\/vnd\.hmrc\.(.*)\+.*""".r)

  def validateContext[T](proxyRequest: ProxyRequest): Future[String] =
    validateOrElse(parseContext(proxyRequest.path), NotFound())

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
