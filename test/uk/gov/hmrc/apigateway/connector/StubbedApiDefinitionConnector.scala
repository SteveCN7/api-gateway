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

import play.api.libs.json.Json.parse
import uk.gov.hmrc.apigateway.connector.impl.ApiDefinitionConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.NotFound
import uk.gov.hmrc.apigateway.model.ApiDefinition
import uk.gov.hmrc.apigateway.play.binding.PlayBindings.apiDefinitionFormat

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source.fromInputStream

class StubbedApiDefinitionConnector extends ApiDefinitionConnector(null) {

  private val stubbedApiDefinitionsCache = mutable.Map[String, ApiDefinition]() // TODO replace this with cache from cache story?

  override def getByContext(context: String): Future[ApiDefinition] = Future {
    stubbedApiDefinitionsCache.get(context).getOrElse {
      val stubbedApiDefinition = loadStubbedApiDefinition(context)
      stubbedApiDefinitionsCache.put(context, stubbedApiDefinition)
      stubbedApiDefinition
    }
  }

  private def loadStubbedApiDefinition(context: String): ApiDefinition = {
    val inputStream = getClass.getResourceAsStream(s"/stub/api-definition/$context.json")
    Option(inputStream).map(fromInputStream).map(_.mkString).map(parse(_).as[ApiDefinition]) match {
      case Some(apiDefinition) => apiDefinition
      case _ => throw NotFound()
    }
  }

}
