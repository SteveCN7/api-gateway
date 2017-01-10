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

package it.uk.gov.hmrc.apigateway.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import it.uk.gov.hmrc.apigateway.{MockHost, Stub}
import play.api.libs.json.Json._
import uk.gov.hmrc.apigateway.model.Application
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._
import play.api.http.Status._

object ThirdPartyApplicationStub extends Stub {
  override val stub = new MockHost(22223)

  def willReturnTheApplicationForServerToken(serverToken: String, application: Application) = {
    stub.mock.register(get(urlPathEqualTo(s"/application")).withHeader("X-server-token", equalTo(serverToken))
      .willReturn(aResponse().withStatus(OK)
        .withBody(stringify(toJson(application)))))
  }

  def willNotReturnAnApplicationForServerToken(serverToken: String) = {
    stub.mock.register(get(urlPathEqualTo(s"/application")).withHeader("X-server-token", equalTo(serverToken))
      .willReturn(aResponse().withStatus(NOT_FOUND)))
  }
}
