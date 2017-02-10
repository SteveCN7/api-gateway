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

import it.uk.gov.hmrc.apigateway.{MockHost, Stub}
import uk.gov.hmrc.apigateway.model.{ApiIdentifier, Application}

object ThirdPartyApplicationStub extends Stub with ThirdPartyApplicationStubMappings {

  override val stub = MockHost(22223)

  def willReturnTheApplicationForServerToken(serverToken: String, application: Application) =
    stub.mock.register(returnTheApplicationForServerToken(serverToken, application))

  def willNotFindAnApplicationForServerToken(serverToken: String) =
    stub.mock.register(willNotFindAnyApplicationForServerToken(serverToken))

  def willFailFindingTheApplicationForServerToken(serverToken: String) =
    stub.mock.register(failFindingTheApplicationForServerToken(serverToken))


  def willReturnTheApplicationForClientId(clientId: String, application: Application) =
    stub.mock.register(returnTheApplicationForClientId(clientId, application))

  def willNotFindAnApplicationForClientId(clientId: String) =
    stub.mock.register(willNotFindAnyApplicationForClientId(clientId))

  def willFailFindingTheApplicationForClientId(clientId: String) =
    stub.mock.register(failFindingTheApplicationForClientId(clientId))


  def willFindTheSubscriptionFor(applicationId: String, api: ApiIdentifier) =
    stub.mock.register(findTheSubscriptionFor(applicationId, api))

  def willNotFindASubscriptionFor(applicationId: String, api: ApiIdentifier) =
    stub.mock.register(willNotFindTheSubscriptionFor(applicationId, api))

  def willFailWhenFetchingTheSubscription(applicationId: String, api: ApiIdentifier) =
    stub.mock.register(failWhenFetchingTheSubscription(applicationId, api))
}
