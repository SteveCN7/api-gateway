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

import uk.gov.hmrc.play.test.UnitSpec

class VaryHeaderKeySpec extends UnitSpec {

  "VaryHeaderKey" should {
    "have a fixed format string representation" in {
      PrimaryCacheKey("/bob/blah", Set("X-Aaa"), Map("X-Aaa" -> Set("aaa"))) shouldBe "/bob/blah::X-Aaa=aaa"
    }

    "have a fixed format string representation with multiple keys" in {
      PrimaryCacheKey("/bob/blah", Set("X-Bbb", "X-Aaa"), Map("X-Bbb" -> Set("bbb"), "X-Aaa" -> Set("aaa"))) shouldBe "/bob/blah::X-Aaa=aaa;X-Bbb=bbb"
    }

    "have a fixed format string representation with multiple keys, where some are missing" in {
      PrimaryCacheKey("/bob/blah", Set("X-Bbb", "X-Aaa"), Map("X-Bbb" -> Set("bbb"))) shouldBe "/bob/blah::X-Aaa=;X-Bbb=bbb"
    }

    "return the original key if there are no required vary headers" in {
      PrimaryCacheKey("/bob/blah", Set.empty, Map("X-Bbb" -> Set("bbb"), "X-Aaa" -> Set("aaa"))) shouldBe "/bob/blah"
    }

    "return the original key suffixed :: if there are no headers matching the vary headers" in {
      PrimaryCacheKey("/bob/blah", Set("X-Aaa"), Map("X-Bbb" -> Set("bbb"))) shouldBe "/bob/blah::X-Aaa="
    }

    "return the original key suffixed :: if headers match only some vary headers" in {
      PrimaryCacheKey("/bob/blah", Set("X-Aaa", "X-Bbb"), Map("X-Bbb" -> Set("bbb"), "X-Ccc" -> Set("ccc"))) shouldBe "/bob/blah::X-Aaa=;X-Bbb=bbb"
    }
  }
}
