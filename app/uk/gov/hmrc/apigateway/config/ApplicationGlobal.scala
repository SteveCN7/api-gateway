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

package uk.gov.hmrc.apigateway.config

import play.api.GlobalSettings
import play.api.libs.json.Json._
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.apigateway.exception.GatewayError.ServiceUnavailable
import play.api.mvc.Results.{ServiceUnavailable => PlayServiceUnavailable, _}
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._

import scala.concurrent.Future

trait ApplicationGlobal extends GlobalSettings {
  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    super.onBadRequest(request, error)
    Future.successful(PlayServiceUnavailable(toJson(ServiceUnavailable())))
  }
}

object ApplicationGlobal extends ApplicationGlobal
