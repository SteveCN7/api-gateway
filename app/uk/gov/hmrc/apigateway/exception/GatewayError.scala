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

package uk.gov.hmrc.apigateway.exception

import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.apigateway.model.ApiRequest

class GatewayError(val code: String, val message: String) extends RuntimeException(message)

object GatewayError {

  case class NotImplemented() extends GatewayError("NOT_IMPLEMENTED", "API has not been implemented")

  case class ServiceNotAvailable() extends GatewayError("SERVER_ERROR", "Service unavailable")

  case class ServerError() extends GatewayError("SERVER_ERROR", "Internal server error")

  case class NotFound() extends GatewayError("NOT_FOUND", "The requested resource could not be found.")

  case class MatchingResourceNotFound() extends GatewayError("MATCHING_RESOURCE_NOT_FOUND", "A resource with the name in the request cannot be found in the API")

  case class InvalidCredentials(request: Request[AnyContent], apiRequest: ApiRequest) extends GatewayError("INVALID_CREDENTIALS", "Invalid Authentication information provided")

  case class MissingCredentials(request: Request[AnyContent], apiRequest: ApiRequest) extends GatewayError("MISSING_CREDENTIALS", "Authentication information is not provided")

  case class IncorrectAccessTokenType() extends GatewayError("INCORRECT_ACCESS_TOKEN_TYPE", "The access token type used is not supported when invoking the API")

  case class InvalidScope() extends GatewayError("INVALID_SCOPE", "Cannot access the required resource. Ensure this token has all the required scopes.")

  case class InvalidSubscription() extends GatewayError("RESOURCE_FORBIDDEN", "The application is not subscribed to the API which it is attempting to invoke")

  case class ThrottledOut() extends GatewayError("MESSAGE_THROTTLED_OUT", "The request for the API is throttled as you have exceeded your quota.")

}
