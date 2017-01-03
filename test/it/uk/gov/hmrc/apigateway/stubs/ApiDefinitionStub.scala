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
import play.api.libs.json.Json.{toJson, stringify}
import uk.gov.hmrc.apigateway.model.ApiDefinition
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._

object ApiDefinitionStub extends Stub {

  override val stub = new MockHost(22221)

  def willReturnTheApiDefinition(apiDefinition: ApiDefinition) = {
    stub.mock.register(get(urlPathEqualTo(s"/api-definition")).withQueryParam("context", equalTo(apiDefinition.context))
      .willReturn(aResponse().withStatus(200)
        .withBody(stringify(toJson(apiDefinition)))))
  }

  def willNotReturnAnApiDefinitionForContext(context: String) = {
    stub.mock.register(get(urlPathEqualTo(s"/api-definition")).withQueryParam("context", equalTo(context))
      .willReturn(aResponse().withStatus(404)))
  }
}
