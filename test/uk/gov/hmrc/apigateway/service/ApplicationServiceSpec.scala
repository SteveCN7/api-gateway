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

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.apigateway.connector.impl.ThirdPartyApplicationConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.{InvalidCredentials, NotFound}
import uk.gov.hmrc.apigateway.model.Application
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val application = Application(UUID.randomUUID(), "App Name")
    val applicationConnector = mock[ThirdPartyApplicationConnector]
    val underTest = new ApplicationService(applicationConnector)
  }

  "get by server token" should {
    val serverToken = "serverToken"

    "return the application when server token is valid" in new Setup {
      when(applicationConnector.getByServerToken(serverToken)).thenReturn(application)
      val result = await(underTest.getByServerToken(serverToken))
      result shouldBe application
    }

    "return an invalid credentials error if the application cannot be fetched" in new Setup {
      when(applicationConnector.getByServerToken(serverToken)).thenReturn(Future.failed(NotFound()))
      intercept[InvalidCredentials] {
        await(underTest.getByServerToken(serverToken))
      }
    }
  }

  "get by client id" should {
    val clientId = "clientId"

    "return the application when an application exists for the given client id" in new Setup {
      when(applicationConnector.getByClientId(clientId)).thenReturn(application)
      val result = await(underTest.getByClientId(clientId))
      result shouldBe application
    }

    "propagate an error when the application cannot be fetched" in new Setup {
      when(applicationConnector.getByClientId(clientId)).thenReturn(Future.failed(NotFound()))
      intercept[NotFound] {
        await(underTest.getByClientId(clientId))
      }
    }
  }
}
