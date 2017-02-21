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

package uk.gov.hmrc.apigateway.service

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import uk.gov.hmrc.apigateway.exception.GatewayError
import uk.gov.hmrc.apigateway.model.AuthType.AuthType
import uk.gov.hmrc.apigateway.model.RateLimitTier.BRONZE
import uk.gov.hmrc.apigateway.model._

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.util.Random

trait RoutingServicesMocks {

  protected def generateRandomAuthType(valueToExclude: AuthType): String = {
    var randomAuthType: AuthType = null
    do {
      randomAuthType = AuthType(Random.nextInt(AuthType.maxId))
    } while (randomAuthType == valueToExclude)
    randomAuthType.toString
  }

  protected def mockAuthority(authorityService: AuthorityService, gatewayError: GatewayError) =
    when(authorityService.findAuthority(any[ProxyRequest])).thenReturn(failed(gatewayError))

  protected def mockAuthority(authorityService: AuthorityService, authority: Authority) =
    when(authorityService.findAuthority(any[ProxyRequest])).thenReturn(successful(authority))

  protected def mockScopeValidation(scopeValidationFilter: ScopeValidator, gatewayError: GatewayError) =
    when(scopeValidationFilter.validate(any(classOf[ThirdPartyDelegatedAuthority]), any(classOf[Option[String]])))
      .thenReturn(failed(gatewayError))

  protected def mockScopeValidation(scopeValidationFilter: ScopeValidator) =
    when(scopeValidationFilter.validate(any(classOf[ThirdPartyDelegatedAuthority]), any(classOf[Option[String]])))
      .thenReturn(successful(()))

  protected def mockApplicationByClientId(applicationService: ApplicationService, clientId: String, gatewayError: GatewayError) =
    when(applicationService.getByClientId(clientId)).thenReturn(failed(gatewayError))

  protected def mockApplicationByClientId(applicationService: ApplicationService, clientId: String, application: Application) =
    when(applicationService.getByClientId(clientId)).thenReturn(successful(application))

  protected def mockApplicationByServerToken(applicationService: ApplicationService,  serverToken: String, gatewayError: GatewayError) =
    when(applicationService.getByServerToken(serverToken)).thenReturn(failed(gatewayError))

  protected def mockApplicationByServerToken(applicationService: ApplicationService, serverToken: String, application: Application) =
    when(applicationService.getByServerToken(serverToken)).thenReturn(successful(application))

  protected def mockValidateSubscriptionAndRateLimit(applicationService: ApplicationService, application: Application, result: Future[Unit]) =
    when(applicationService.validateSubscriptionAndRateLimit(refEq(application), any[ApiIdentifier]())).thenReturn(result)

  protected def anApplication(): Application =
    Application(id = UUID.randomUUID(), clientId = "clientId", name = "appName", rateLimitTier = BRONZE)

  protected def validAuthority(): Authority = {
    val token = Token("accessToken", Set.empty, DateTime.now.plusMinutes(5))
    val thirdPartyDelegatedAuthority = ThirdPartyDelegatedAuthority("authBearerToken", "clientId", token, Some(UserData("userOID")))
    Authority(thirdPartyDelegatedAuthority, authExpired = false)
  }

}
