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
      out shouldBe model.CacheControl(false, None, None)
    }

    "have noCache set to true when a no-cache header is provided" in {
      CacheControl.fromHeaders(Map(HeaderNames.CACHE_CONTROL -> Seq("no-cache")))
        .shouldBe(model.CacheControl(true, None, None))

      CacheControl.fromHeaders(Map(HeaderNames.CACHE_CONTROL -> Seq("no-cache", "no-store")))
        .shouldBe(model.CacheControl(true, None, None))

      CacheControl.fromHeaders(Map(HeaderNames.CACHE_CONTROL -> Seq("no-store", "no-cache")))
        .shouldBe(model.CacheControl(true, None, None))
    }

    "have maxAge set to the appropriate value when a cache header with max-age is provided" in {
      CacheControl.fromHeaders(Map(HeaderNames.CACHE_CONTROL -> Seq("max-age=123")))
        .shouldBe(model.CacheControl(false, Some(123), None))

//      CacheControl.fromHeaders(Map(HeaderNames.CACHE_CONTROL -> Seq("no-cache", "no-store")))
//        .shouldBe(model.CacheControl(false, None, None))
//
//      CacheControl.fromHeaders(Map(HeaderNames.CACHE_CONTROL -> Seq("no-store", "no-cache")))
//        .shouldBe(model.CacheControl(false, None, None))
    }
  }
}
