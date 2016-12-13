package uk.gov.hmrc.play.binding

import play.api.libs.json._
import uk.gov.hmrc.model.{ApiDefinition, ApiEndpoint, ApiVersion}
import uk.gov.hmrc.exception.GatewayError

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
