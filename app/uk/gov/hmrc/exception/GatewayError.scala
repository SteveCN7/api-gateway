package uk.gov.hmrc.exception

class GatewayError(val code: String, val message: String) extends Throwable(message)

object GatewayError {

  case class ServerError() extends GatewayError("SERVER_ERROR", "Service unavailable")

  case class ContextNotFound() extends GatewayError("NOT_FOUND", "Requested resource could not be found")

  case class MatchingResourceNotFound() extends GatewayError("MATCHING_RESOURCE_NOT_FOUND", "A resource with the name in the request cannot be found in the API")

  case class InvalidAcceptHeader() extends GatewayError("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid")

}
