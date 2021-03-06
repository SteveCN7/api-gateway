/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.mvc._

object PlayRequestUtils {

  def bodyOf(request: Request[AnyContent]): Option[String] = {
    request.body match {
      case AnyContentAsJson(json) => Some(Json.stringify(json))
      case AnyContentAsText(txt) => Some(txt)
      case AnyContentAsXml(xml) => Some(xml.toString())
      case _ => None
    }
  }

  def asMapOfSets(seqOfPairs: Seq[(String, String)]): Map[String, Set[String]] =
    seqOfPairs
      .groupBy(_._1)
      .mapValues(_.map(_._2).toSet)

  def replaceHeaders(headers: Headers)(updatedHeaders: (String, Option[String])*): Headers = {
    updatedHeaders.headOption match {
      case Some((headerName, Some(headerValue))) => replaceHeaders(headers.replace(headerName -> headerValue))(updatedHeaders.tail:_*)
      case Some((headerName, None)) => replaceHeaders(headers.remove(headerName))(updatedHeaders.tail:_*)
      case None => headers
    }
  }
}
