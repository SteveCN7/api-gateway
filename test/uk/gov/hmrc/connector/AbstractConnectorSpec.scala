package uk.gov.hmrc.connector

import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.successful

class AbstractConnectorSpec extends UnitSpec with MockitoSugar {

  private val wsClient = mock[WSClient]
  private val wsRequest = mock[WSRequest]

  private val abstractConnectorImpl = new AbstractConnectorImpl {
    when(wsClient.url(anyString)).thenReturn(wsRequest)
  }

  "Abstract connector" should {

    "throw a runtime exception when the response is '404' not found" in {
      mockWsClientToReturn(NOT_FOUND)
      intercept[RuntimeException] {
        await(abstractConnectorImpl.get[String]("http://host.example/foo/bar"))
      }
    }

    "return response json payload when the response is '2xx'" in {
      implicit val apiDefinitionFormat = Json.format[Foo]
      mockWsClientToReturn(OK, """ { "bar" : "baz" } """)
      val result = await(abstractConnectorImpl.get[Foo]("http://host.example/foo/bar"))
      result shouldBe Foo("baz")
    }

  }

  private def mockWsClientToReturn(statusCode: Int, responseJson: String = "{}") = {
    val wsResponse = mock[WSResponse]
    when(wsResponse.status).thenReturn(statusCode)
    when(wsResponse.json).thenReturn(parse(responseJson))
    when(wsRequest.get()).thenReturn(successful(wsResponse))
  }

  class AbstractConnectorImpl extends AbstractConnector(wsClient)

  case class Foo(bar: String)

}
