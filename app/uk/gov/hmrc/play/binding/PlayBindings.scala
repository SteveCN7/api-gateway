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

package uk.gov.hmrc.play.binding

import play.api.libs.json._
import uk.gov.hmrc.exception.GatewayError
import uk.gov.hmrc.model.{ApiDefinition, ApiEndpoint, ApiVersion}

object PlayBindings {

  implicit val gatewayErrorWrites = Writes[GatewayError] { gatewayError =>
    JsObject(Seq(
      "code" -> JsString(gatewayError.code),
      "message" -> JsString(gatewayError.message)
    ))
  }

  implicit val apiEndpointFormat = Json.format[ApiEndpoint]
  implicit val apiVersionFormat = Json.format[ApiVersion]
  implicit val apiDefinitionFormat = Json.format[ApiDefinition]

}
