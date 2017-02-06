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

import org.joda.time.{DateTimeUtils, DateTime}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.play.test.UnitSpec

class TimeSpec extends UnitSpec with BeforeAndAfterEach {

  override def beforeEach {
    DateTimeUtils.setCurrentMillisFixed(new DateTime("2012-08-16T07:22:05Z").getMillis)
  }

  override def afterEach {
    DateTimeUtils.setCurrentMillisSystem()
  }

  "minutesSinceEpoch" should {
    "return the number of minutes since epoch" in {
      Time.minutesSinceEpoch() shouldBe 22418362
    }

    "return the same number of minutes at :00s and :059s" in {
      DateTimeUtils.setCurrentMillisFixed(new DateTime("2012-08-16T07:22:00Z").getMillis)
      val minutesSinceEpochAt00s = Time.minutesSinceEpoch()

      DateTimeUtils.setCurrentMillisFixed(new DateTime("2012-08-16T07:22:59Z").getMillis)
      val minutesSinceEpochAt59s = Time.minutesSinceEpoch()

      minutesSinceEpochAt00s shouldBe minutesSinceEpochAt59s
    }
  }
}
