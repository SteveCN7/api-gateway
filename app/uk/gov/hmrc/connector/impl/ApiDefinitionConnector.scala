package uk.gov.hmrc.connector.impl

import javax.inject.{Inject, Singleton}

import play.api.libs.ws.WSClient
import uk.gov.hmrc.connector.ServiceConnector
import uk.gov.hmrc.exception.GatewayError.ContextNotFound
import uk.gov.hmrc.model.ApiDefinition
import uk.gov.hmrc.play.binding.PlayBindings.apiDefinitionFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ApiDefinitionConnector @Inject()(wsClient: WSClient)
  extends ServiceConnector(wsClient, "api-definition") {

  def getByContext(context: String): Future[ApiDefinition] =
    get[ApiDefinition](s"$serviceName?context=$context") recover {
      case error: RuntimeException => throw ContextNotFound()
    }

}
