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
import play.api.http.Status.OK
import play.mvc.Http.HeaderNames.USER_AGENT

object ApiStub extends Stub {

  val port = 22220
  val url = s"http://localhost:$port"

  override val stub = MockHost(port)

  def willReturnTheResponse(response: String) = {
    stub.mock.register(
      get(anyUrl())
        .withHeader(USER_AGENT, equalTo("api-gateway"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(response)))
  }
}
