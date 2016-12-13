package uk.gov.hmrc.play.filter

import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.connector.impl.ApiDefinitionConnector
import uk.gov.hmrc.model._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.util.HttpHeaders.ACCEPT

import scala.concurrent.Future
import scala.concurrent.Future._

class EndpointMatchFilterSpec extends UnitSpec with MockitoSugar {

  private val apiDefinitionConnector = mock[ApiDefinitionConnector]
  private val endpointMatchFilter = new EndpointMatchFilter(apiDefinitionConnector)
  private val apiDefinition = ApiDefinition(
    "api-context", "http://host.example", Seq(ApiVersion("1.0", Seq(ApiEndpoint("/api-endpoint", "GET"))))
  )

  "Endpoint match filter" should {

    val proxyRequest = ProxyRequest("GET", "/api-context/api-endpoint", Map(ACCEPT -> "application/vnd.hmrc.1.0+json"))
    val apiDefinitionMatch = ApiDefinitionMatch("api-context", "http://host.example", "1.0", None)

    "invoke api definition connector with correct service name" in {
      mockApiServiceConnectorToReturnSuccess
      await(endpointMatchFilter.filter(proxyRequest))
      verify(apiDefinitionConnector).getByContext("api-context")
    }

    "return api definition when proxy request matches api definition endpoint" in {
      mockApiServiceConnectorToReturnSuccess
      await(endpointMatchFilter.filter(proxyRequest)) shouldBe apiDefinitionMatch
    }

    "throw an exception when proxy request does not match api definition endpoint" in {
      mockApiServiceConnectorToReturnFailure
      intercept[RuntimeException] {
        await(endpointMatchFilter.filter(proxyRequest))
      }
    }

  }

  private def mockApiServiceConnectorToReturnSuccess =
    mockApiServiceConnectorToReturn("api-context", successful(apiDefinition))

  private def mockApiServiceConnectorToReturnFailure =
    mockApiServiceConnectorToReturn("api-context", failed(new RuntimeException("simulated test exception")))

  private def mockApiServiceConnectorToReturn(serviceName: String, eventualApiDefinition: Future[ApiDefinition]) =
    when(apiDefinitionConnector.getByContext(serviceName)).thenReturn(eventualApiDefinition)

}
