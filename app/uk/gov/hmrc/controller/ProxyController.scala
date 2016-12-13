package uk.gov.hmrc.controller

import javax.inject.{Inject, Singleton}

import play.Logger
import play.api.libs.json.Json.toJson
import play.api.mvc.Results._
import play.api.mvc.{Action, BodyParsers}
import uk.gov.hmrc.exception.GatewayError._
import uk.gov.hmrc.play.binding.PlayBindings._
import uk.gov.hmrc.service.ProxyService

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ProxyController @Inject()(proxyService: ProxyService) {

  def proxy = Action.async(BodyParsers.parse.anyContent) { implicit request =>
    proxyService.proxy(request) recover {
      case error => {
        Logger.error("unexpected error", error)
        InternalServerError(toJson(ServerError()))
      }
    }
  }

}
