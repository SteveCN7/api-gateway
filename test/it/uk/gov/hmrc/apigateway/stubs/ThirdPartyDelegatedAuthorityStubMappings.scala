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
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json.{stringify, toJson}
import uk.gov.hmrc.apigateway.model.Authority
import uk.gov.hmrc.apigateway.play.binding.PlayBindings._
import uk.gov.hmrc.apigateway.util.HttpHeaders.USER_AGENT

trait ThirdPartyDelegatedAuthorityStubMappings {

  protected def returnTheAuthorityForAccessToken(accessToken: String, authority: Authority): MappingBuilder =
    get(urlPathEqualTo("/authority"))
      .withHeader(USER_AGENT, equalTo("api-gateway"))
      .withQueryParam("access_token", equalTo(accessToken))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(stringify(toJson(authority)))
      )

  protected def doNotReturnAnAuthorityForAccessToken(accessToken: String): MappingBuilder =
    get(urlPathEqualTo("/authority"))
      .withHeader(USER_AGENT, equalTo("api-gateway"))
      .withQueryParam("access_token", equalTo(accessToken))
      .willReturn(
        aResponse().withStatus(NOT_FOUND)
      )

}
