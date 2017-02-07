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

package uk.gov.hmrc.apigateway.connector

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.json.Json.{stringify, toJson}
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.apigateway.exception.GatewayError.NotFound
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class AbstractConnectorSpec extends UnitSpec with WithFakeApplication with BeforeAndAfterEach {

  val stubPort = sys.env.getOrElse("WIREMOCK", "22224").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  trait Setup {
    val underTest = new AbstractConnectorImpl(fakeApplication.injector.instanceOf[WSClient])
  }

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  "asMapOfSeq" should {
    "convert a simple seq into a map"  in new Setup(){
      val res = underTest.asMapOfSeq(Seq("A" -> "aaa", "B" -> "bbb"))
      res shouldBe(Map("A" -> Set("aaa"), "B" -> Set("bbb")))
    }

    "convert a complex seq into a map"  in new Setup(){
      val res = underTest.asMapOfSeq(Seq(
        "A" -> "aaa",
        "B" -> "bbb",
        "B" -> "yyy,qqq",
        "A" -> "xxx, zzz"
      ))
      res shouldBe(Map("A" -> Set("aaa", "xxx, zzz"), "B" -> Set("bbb", "yyy,qqq")))
    }
  }

  "Abstract connector" should {

    "throw a not found error when the response is '404' not found" in new Setup {

      stubFor(get(urlPathEqualTo("/foo/bar"))
        .willReturn(
          aResponse().withStatus(NOT_FOUND)
        ))

      intercept[NotFound] {
        await(underTest.get[String](s"$wireMockUrl/foo/bar"))
      }
    }

    "return response json payload when the response is '2xx'" in new Setup {
      implicit val apiDefinitionFormat = Json.format[Foo]

      stubFor(get(urlPathEqualTo("/foo/bar"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(stringify(toJson(Foo("bar"))))
        ))

      val result = await(underTest.get[Foo](s"$wireMockUrl/foo/bar"))

      result shouldBe Foo("bar")
    }

    "add headers to the request when provided" in new Setup {
      implicit val apiDefinitionFormat = Json.format[Foo]

      stubFor(get(urlPathEqualTo("/foo/bar")).withHeader("foo", equalTo("bar"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(stringify(toJson(Foo("bar"))))
        ))

      val result = await(underTest.get[Foo](s"$wireMockUrl/foo/bar", Seq(("foo", "bar"))))

      result._1 shouldBe Foo("bar")
    }

    "handle comma delimited headers from the response when provided" in new Setup {
      implicit val apiDefinitionFormat = Json.format[Foo]

      stubFor(get(urlPathEqualTo("/foo/bar")).withHeader("foo", equalTo("bar"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(stringify(toJson(Foo("bar"))))
            .withHeader(HeaderNames.CACHE_CONTROL, "no-cache,max-age=0,no-store")
            .withHeader(HeaderNames.VARY, "X-Blah")
            .withHeader(HeaderNames.VARY, "X-Bling")
            .withHeader(HeaderNames.VARY, "X-Blit, X-Blat")
        ))

      val result = await(underTest.get[Foo](s"$wireMockUrl/foo/bar", Seq(("foo", "bar"))))

      result._1 shouldBe Foo("bar")

      private val cacheControl = result._2.getOrElse(HeaderNames.CACHE_CONTROL, Set.empty)
      private val vary = result._2.getOrElse(HeaderNames.VARY, Nil)

      cacheControl.size shouldBe 1
      cacheControl should contain("no-cache,max-age=0,no-store")

      vary should contain("X-Blah")
      vary should contain("X-Bling")
      vary should contain("X-Blit, X-Blat")
    }
  }

  class AbstractConnectorImpl(wsClient: WSClient) extends AbstractConnector(wsClient)
  case class Foo(bar: String)
}
