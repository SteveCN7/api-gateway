/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.apigateway.connector

import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.when
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSRequest}
import uk.gov.hmrc.apigateway.exception.GatewayError.NotFound
import uk.gov.hmrc.play.test.UnitSpec

class AbstractConnectorSpec extends UnitSpec with WsClientMocking {

  private val wsClient = mock[WSClient]
  private val wsRequest = mock[WSRequest]

  private val abstractConnectorImpl = new AbstractConnectorImpl {
    when(wsClient.url(anyString)).thenReturn(wsRequest)
  }

  "Abstract connector" should {

    "throw a runtime exception when the response is '404' not found" in {
      mockWsClient(wsClient, "http://host.example/foo/bar", NOT_FOUND)
      intercept[NotFound] {
        await(abstractConnectorImpl.get[String]("http://host.example/foo/bar"))
      }
    }

    "return response json payload when the response is '2xx'" in {
      implicit val apiDefinitionFormat = Json.format[Foo]
      mockWsClient(wsClient, "http://host.example/foo/bar", OK, """ { "bar" : "baz" } """)
      val result = await(abstractConnectorImpl.get[Foo]("http://host.example/foo/bar"))
      result shouldBe Foo("baz")
    }

  }

  class AbstractConnectorImpl extends AbstractConnector(wsClient)

  case class Foo(bar: String)

}
