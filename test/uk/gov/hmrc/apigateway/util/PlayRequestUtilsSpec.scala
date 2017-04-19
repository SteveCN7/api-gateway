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

package uk.gov.hmrc.apigateway.util

import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.util.HttpHeaders._
import uk.gov.hmrc.play.http.HeaderNames.xRequestId
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.XML

class PlayRequestUtilsSpec extends UnitSpec {

  "bodyOf" should {

    "return the body of a json request" in {
      val json = """{"body":"test"}"""
      val request = FakeRequest().withBody(AnyContentAsJson(Json.parse(json)))

      val result = PlayRequestUtils.bodyOf(request)

      result shouldBe Some(json)
    }

    "return the body of a text request" in {
      val request = FakeRequest().withBody(AnyContentAsText("text"))

      val result = PlayRequestUtils.bodyOf(request)

      result shouldBe Some("text")
    }

    "return the body of a xml request" in {
      val xml = """<xml>test</xml>"""
      val request = FakeRequest().withBody(AnyContentAsXml(XML.loadString(xml)))

      val result = PlayRequestUtils.bodyOf(request)

      result shouldBe Some(xml)
    }

    "return None when no valid body" in {
      val xml = """<xml>test</xml>"""
      val request = FakeRequest().withBody(AnyContentAsEmpty)

      val result = PlayRequestUtils.bodyOf(request)

      result shouldBe None
    }
  }

  "asMapOfSeq" should {
    "convert a simple seq into a map" in {
      val res = PlayRequestUtils.asMapOfSets(Seq("A" -> "aaa", "B" -> "bbb"))
      res shouldBe Map("A" -> Set("aaa"), "B" -> Set("bbb"))
    }

    "convert a complex seq into a map"  in {
      val res = PlayRequestUtils.asMapOfSets(Seq(
        "A" -> "aaa",
        "B" -> "bbb",
        "B" -> "yyy,qqq",
        "A" -> "xxx, zzz"
      ))
      res shouldBe Map("A" -> Set("aaa", "xxx, zzz"), "B" -> Set("bbb", "yyy,qqq"))
    }
  }

  "replaceHeaders" should {
    val headers = Headers(xRequestId -> "requestId")

    "add new headers" in {
      val res = PlayRequestUtils.replaceHeaders(headers)((X_CLIENT_ID, Some("clientId")))
      res.headers shouldBe Seq((xRequestId, "requestId"), (X_CLIENT_ID, "clientId"))
    }

    "replace existing headers" in {
      val res = PlayRequestUtils.replaceHeaders(headers)((xRequestId, Some("newRequestId")))
      res.headers shouldBe Seq((xRequestId, "newRequestId"))
    }

    "remove headers" in {
      val res = PlayRequestUtils.replaceHeaders(headers)((xRequestId, None))
      res.headers shouldBe Seq()
    }

    "leave headers intact" in {
      val res = PlayRequestUtils.replaceHeaders(headers)()
      res.headers shouldBe Seq((xRequestId, "requestId"))
    }
  }
}
