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

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status._
import play.api.libs.json.Json._
import uk.gov.hmrc.apigateway.model.{API, Application}
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._
import uk.gov.hmrc.apigateway.util.HttpHeaders._

trait ThirdPartyApplicationStubMappings {

  protected def returnTheApplicationForServerToken(serverToken: String, application: Application): MappingBuilder =
    get(urlPathEqualTo("/application"))
      .withHeader(X_API_GATEWAY_SERVER_TOKEN, equalTo(serverToken))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(stringify(toJson(application)))
      )

  protected def willNotFindAnyApplicationForServerToken(serverToken: String): MappingBuilder =
    get(urlPathEqualTo("/application"))
      .withHeader(X_API_GATEWAY_SERVER_TOKEN, equalTo(serverToken))
      .willReturn(
        aResponse().withStatus(NOT_FOUND)
      )

  protected def returnTheApplicationForClientId(clientId: String, application: Application): MappingBuilder =
    get(urlPathEqualTo("/application"))
      .withQueryParam("clientId", equalTo(clientId))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(stringify(toJson(application)))
      )

  protected def willNotFindAnyApplicationForClientId(clientId: String): MappingBuilder =
    get(urlPathEqualTo("/application"))
      .withQueryParam("clientId", equalTo(clientId))
      .willReturn(
        aResponse().withStatus(NOT_FOUND)
      )

  protected def returnTheSubscriptionsForApplicationId(applicationId: String, subscriptions: Seq[API]): MappingBuilder =
    get(urlPathEqualTo(s"/application/$applicationId/subscription"))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(stringify(toJson(subscriptions)))
      )

  protected def willNotFindAnySubscriptionForApplicationId(applicationId: String): MappingBuilder =
    get(urlPathEqualTo(s"/application/$applicationId/subscription"))
      .willReturn(
        aResponse().withStatus(NOT_FOUND)
      )

}
