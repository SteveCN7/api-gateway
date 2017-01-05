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

import javax.inject.{Singleton, Inject}

import com.typesafe.config.Config
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.{Application, Configuration}
import play.twirl.api.Html
import uk.gov.hmrc.apigateway.exception.GatewayError.ServerError
import uk.gov.hmrc.play.audit.filters.AuditFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter

@Singleton
class ControllerConfiguration @Inject() (configuration: Configuration) extends ControllerConfig {
  import net.ceedubs.ficus.Ficus._
  lazy val controllerConfigs = configuration.underlying.as[Config]("controllers")
}

@Singleton
class MicroserviceAuditFilter @Inject() (controllerConfiguration: ControllerConfiguration) extends AuditFilter with AppName with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceAuditConnector
  override def controllerNeedsAuditing(controllerName: String) = controllerConfiguration.paramsForController(controllerName).needsAuditing
}

@Singleton
class MicroserviceLoggingFilter @Inject() (controllerConfiguration: ControllerConfiguration) extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = controllerConfiguration.paramsForController(controllerName).needsLogging
}

@Singleton
class MicroserviceGlobal @Inject() (override val loggingFilter: MicroserviceLoggingFilter,
                                    override val frontendAuditFilter: FrontendAuditFilter) extends DefaultFrontendGlobal with RunMode {

  override val auditConnector = MicroserviceAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig("microservice.metrics")

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    Html(Json.toJson(ServerError()).toString)
}
