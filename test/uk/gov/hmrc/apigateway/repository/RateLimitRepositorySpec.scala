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

package uk.gov.hmrc.apigateway.repository

import org.joda.time.DateTime.now
import org.joda.time.DateTimeUtils.setCurrentMillisFixed
import org.joda.time.DateTimeUtils
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceFakeApplicationFactory
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.ReactiveMongoApi
import uk.gov.hmrc.apigateway.exception.GatewayError.ThrottledOut
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.ExecutionContext.Implicits.global

class RateLimitRepositorySpec extends UnitSpec with GuiceFakeApplicationFactory with MongoSpecSupport with BeforeAndAfterEach {

  private val application = new GuiceApplicationBuilder()
    .configure(
      "mongodb.uri" -> mongoUri,
      "mongodb.w" -> 1,
      "mongodb.j" -> false
    ).build()
  val reactiveMongo = application.injector.instanceOf[ReactiveMongoApi]
  val underTest = application.injector.instanceOf[RateLimitRepository]

  override def beforeEach(): Unit = {
    await(reactiveMongo.database.map(_.drop()))
    await(underTest.ensureIndexes())
    setCurrentMillisFixed(1000)
  }

  override def afterEach(): Unit = {
    await(reactiveMongo.database.map(_.drop()))
    DateTimeUtils.setCurrentMillisSystem()
  }

  "validateAndIncrement" should {

    "return successfully and increment when the threshold is not reached" in {

      val result = await(underTest.validateAndIncrement("clientId", 10))

      result shouldBe ()
    }

    "fail when the threshold is reached" in {

      await(underTest.validateAndIncrement("clientId", 2))
      await(underTest.validateAndIncrement("clientId", 2))

      intercept[ThrottledOut] {
        await(underTest.validateAndIncrement("clientId", 2))
      }
    }

    "reset the threshold when a minute has passed" in {

      await(underTest.validateAndIncrement("clientId", 2))
      await(underTest.validateAndIncrement("clientId", 2))
      setCurrentMillisFixed(now().plusMinutes(1).getMillis)

      val result = await(underTest.validateAndIncrement("clientId", 2))

      result shouldBe ()
    }
  }
}
