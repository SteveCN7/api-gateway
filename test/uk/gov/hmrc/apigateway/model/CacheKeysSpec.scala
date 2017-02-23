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

class CacheKeysSpec extends UnitSpec {

  "VaryHeaderKey" should {
    "have a fixed format string representation" in {
      PrimaryCacheKey("/bob/blah", Some("X-Aaa"), Map("X-Aaa" -> Set("aaa"))) shouldBe "/bob/blah::X-Aaa=aaa"
    }

    "return the original key if there are no required vary headers" in {
      PrimaryCacheKey("/bob/blah", None, Map("X-Bbb" -> Set("bbb"), "X-Aaa" -> Set("aaa"))) shouldBe "/bob/blah"
    }

    "return the original key suffixed ::X-Aaa if there are no headers matching the vary headers" in {
      PrimaryCacheKey("/bob/blah", Some("X-Aaa"), Map("X-Bbb" -> Set("bbb"))) shouldBe "/bob/blah::X-Aaa="
    }
  }
}
