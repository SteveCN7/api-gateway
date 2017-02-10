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

package uk.gov.hmrc.apigateway.play.binding

import play.api.libs.json._
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.model._

object PlayBindings {

  implicit val gatewayErrorWrites = Writes[GatewayError] { gatewayError =>
    JsObject(Seq(
      "code" -> JsString(gatewayError.code),
      "message" -> JsString(gatewayError.message)
    ))
  }

  implicit val authTypeFormat = EnumJson.enumFormat(AuthType)
  implicit val parameterFormat = Json.format[Parameter]
  implicit val apiEndpointFormat = Json.format[ApiEndpoint]
  implicit val apiVersionFormat = Json.format[ApiVersion]
  implicit val apiDefinitionFormat = Json.format[ApiDefinition]
  implicit val rateLimitTierFormat = EnumJson.enumFormat(RateLimitTier)
  implicit val applicationFormat = Json.format[Application]
  implicit val versionFormat = Json.format[Version]
  implicit val subscriptionFormat = Json.format[Subscription]
  implicit val apiFormat = Json.format[Api]
  implicit val apiIdentifierFormat = Json.format[ApiIdentifier]

  implicit val tokenFormat = Json.format[Token]
  implicit val userDataFormat = Json.format[UserData]
  implicit val thirdPartyDelegatedAuthorityFormat = Json.format[ThirdPartyDelegatedAuthority]
  implicit val authorityFormat = Json.format[Authority]

}

// TODO copied from https://github.tools.tax.service.gov.uk/HMRC/third-party-application/blob/master/app/uk/gov/hmrc/models/JsonFormatters.scala#L100-L126 could be in a library
object EnumJson {

  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException =>
            throw new InvalidEnumException(enum.getClass.getSimpleName, s)
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }

}

class InvalidEnumException(className: String, input:String) extends RuntimeException(s"Enumeration expected of type: '$className', but it does not contain '$input'")
