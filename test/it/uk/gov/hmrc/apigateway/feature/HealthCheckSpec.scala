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

package it.uk.gov.hmrc.apigateway.feature

import it.uk.gov.hmrc.apigateway.BaseFeatureSpec
import play.api.http.Status._

import scalaj.http.Http

class HealthCheckSpec extends BaseFeatureSpec {

  feature("Health Check for Docktor") {

    scenario("Ping should return 200 (OK) when the service is up and running") {

      When("We call /ping/ping")
      val httpResponse = invoke(Http(s"$serviceUrl/ping/ping"))

      Then("The http response is '200' unauthorized")
      assertCodeIs(httpResponse, OK)
    }
  }
}
