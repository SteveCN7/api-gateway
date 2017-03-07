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
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.duration._
import scalaj.http.Http

class StefanoSpec extends UnitSpec with WithFakeApplication with BeforeAndAfterEach {

  private val stubPort = sys.env.getOrElse("WIREMOCK", "22345").toInt
  private val stubHost = "localhost"
  private val wireMockUrl = s"http://$stubHost:$stubPort"
  private val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  //implicit private val maximumWaitingTime = 45.seconds

  override def beforeEach {
//    wireMockServer.resetMappings()
//    WireMock.reset()
//    wireMockServer.start()
//    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach {
//    wireMockServer.stop()
  }

  "WireMock" should {

    "stub get request" in {

//      val path = "/gzip"
//
//      stubFor(get(urlEqualTo(path))
//        .willReturn(
//          aResponse()
//            .withStatus(200)
//            .withFixedDelay(5900)
//        )
//      )
//
//      val response = Http(s"$wireMockUrl$path").asString
//
//      response.code shouldBe 200
//
//      println(" == ")
//      println(response.getClass)
//      println(response.headers)
//      println(response)
//      println(response.body)

    }
  }

}
