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

package uk.gov.hmrc.apigateway.connector

import java.net.URL
import javax.inject.{Inject, Singleton}

import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, Request, Result, Results}
import uk.gov.hmrc.apigateway.connector.impl.ProxyConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.NotFound

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source._

@Singleton
class StubbedProxyConnector @Inject()(wsClient: WSClient) extends ProxyConnector(wsClient) {

  override def proxy(request: Request[AnyContent], destinationUrl: String): Future[Result] = Future {
    val path = new URL(destinationUrl).getPath.substring(2)
    Results.Ok(loadStubbedProxyConnectorResponse(path))
  }

  private def loadStubbedProxyConnectorResponse(path: String): String = {
    val inputStream = getClass.getResourceAsStream(s"/stub/$path.json")
    Option(inputStream).map(fromInputStream).map(_.mkString) match {
      case Some(jsonString) => jsonString
      case _ => throw NotFound()
    }
  }

}
