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

package uk.gov.hmrc.apigateway.model

import play.mvc.Http.HeaderNames
import uk.gov.hmrc.apigateway.model
import uk.gov.hmrc.play.test.UnitSpec

class CacheControlSpec extends UnitSpec {

  "CacheControl" should {
    "have default parameters when there are no headers" in {
      val out = CacheControl.fromHeaders(Map.empty)
      out shouldBe model.CacheControl(false, None, Set.empty[String])
    }

    "have noCache set to true when a no-cache header is provided" in {
      CacheControl.fromHeaders(Map(HeaderNames.CACHE_CONTROL -> Set("no-cache")))
        .shouldBe(model.CacheControl(true, None, Set.empty[String]))

      CacheControl.fromHeaders(Map(HeaderNames.CACHE_CONTROL -> Set("no-cache", "no-store")))
        .shouldBe(model.CacheControl(true, None, Set.empty[String]))

      CacheControl.fromHeaders(Map(HeaderNames.CACHE_CONTROL -> Set("no-store", "no-cache")))
        .shouldBe(model.CacheControl(true, None, Set.empty[String]))
    }

    "have maxAge set to the appropriate value when a cache header with max-age is provided" in {
      CacheControl.fromHeaders(Map(HeaderNames.CACHE_CONTROL -> Set("max-age=123")))
        .shouldBe(model.CacheControl(false, Some(123), Set.empty[String]))

      CacheControl.fromHeaders(Map(HeaderNames.CACHE_CONTROL -> Set("no-transform", "max-age=234", "min-fresh=77")))
        .shouldBe(model.CacheControl(false, Some(234), Set.empty[String]))
    }

    "set both no-cache and max-age values when appropriate headers are provided" in {
      CacheControl.fromHeaders(Map(
        HeaderNames.CACHE_CONTROL -> Set("no-transform", "max-age=0", "no-cache"),
        HeaderNames.CONTENT_LENGTH -> Set("700")
      ))
        .shouldBe(model.CacheControl(true, Some(0), Set.empty[String]))
    }

    "set the Vary property when a Vary header is provided" in {
      CacheControl.fromHeaders(Map(HeaderNames.VARY -> Set("X-Blah")))
        .shouldBe(model.CacheControl(false, None, Set("X-Blah")))
    }

    "set the Vary property when multiple Vary headers are provided" in {
      val output = CacheControl.fromHeaders(Map(HeaderNames.VARY -> Set("X-Blah", "X-Blob")))
      output.noCache shouldBe false
      output.maxAgeSeconds shouldBe None
      output.vary should contain("X-Blah")
      output.vary should contain("X-Blob")
    }

    "set the all the properties when multiple headers are provided - 1" in {
      val output = CacheControl.fromHeaders(Map(
        HeaderNames.CACHE_CONTROL -> Set("no-transform", "max-age=0", "no-cache"),
        HeaderNames.CONTENT_LENGTH -> Set("700"),
        HeaderNames.VARY -> Set("X-Blah", "X-Blob")
      ))
      output.noCache shouldBe true
      output.maxAgeSeconds shouldBe Some(0)
      output.vary should contain("X-Blah")
      output.vary should contain("X-Blob")
    }

    "set the all the properties when multiple headers are provided - 2" in {
      val output = CacheControl.fromHeaders(Map(
        HeaderNames.CACHE_CONTROL -> Set("max-age=77"),
        HeaderNames.CONTENT_LENGTH -> Set("700"),
        HeaderNames.VARY -> Set("X-Blah", "X-Blob")
      ))
      output.noCache shouldBe false
      output.maxAgeSeconds shouldBe Some(77)
      output.vary should contain("X-Blah")
      output.vary should contain("X-Blob")
    }
  }
}
