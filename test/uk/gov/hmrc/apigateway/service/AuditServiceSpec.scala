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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import controllers.Default
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils.{setCurrentMillisFixed, setCurrentMillisSystem}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, AnyContentAsText}
import play.api.test.FakeRequest
import uk.gov.hmrc.apigateway.connector.impl.MicroserviceAuditConnector
import uk.gov.hmrc.apigateway.util.RequestTags._
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.{DataCall, MergedDataEvent}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class AuditServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  trait Setup {
    implicit val hc = HeaderCarrier()
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()
    val configuration = mock[Configuration]
    val auditConnector = mock[MicroserviceAuditConnector]

    when(auditConnector.sendMergedEvent(any())(any(), any())).thenReturn(AuditResult.Success)
    when(configuration.getInt("auditBodySizeLimit")).thenReturn(Some(100))

    val underTest = new AuditService(configuration, auditConnector, materializer)
  }

  override def beforeEach() = {
    setCurrentMillisFixed(10000)
  }

  override def afterEach() = {
    setCurrentMillisSystem()
  }

  "auditSuccessful" should {
    val captor = ArgumentCaptor.forClass(classOf[MergedDataEvent])
    val requestTimestamp = new DateTime("2017-01-01")

    "send an audit event" in new Setup {

      val request = FakeRequest("POST", "/hello/user")
        .withBody(AnyContentAsJson(Json.parse("""{"body":"test"}""")))
        .copyFakeRequest(
          tags = Map(
            AUTH_TYPE -> "USER",
            API_CONTEXT -> "hello",
            API_VERSION -> "1.0",
            OAUTH_AUTHORIZATION-> "Bearer accessToken",
            REQUEST_TIMESTAMP_MILLIS -> requestTimestamp.getMillis.toString,
            CLIENT_ID -> "applicationClientId",
            USER_OID -> "userOid"
          ),
          remoteAddress = "10.10.10.10"
        )
        .withHeaders("X-Request-ID" -> "requestId")

      val result = await(underTest.auditSuccessfulRequest(request, Default.Ok("responseBody")))

      verify(auditConnector).sendMergedEvent(captor.capture())(any(), any())
      val auditedEvent = captor.getValue.asInstanceOf[MergedDataEvent]

      auditedEvent shouldBe MergedDataEvent(
        auditSource = "api-gateway",
        auditType = "APIGatewayRequestCompleted",
        eventId = auditedEvent.eventId,
        request = DataCall(
          tags = Map(
            "X-Request-ID" -> "requestId",
            "path" -> "/hello/user",
            "transactionName" -> "Request has been completed via the API Gateway",
            "clientIP" -> "10.10.10.10",
            "clientPort" -> "443",
            "type" -> "Audit"
          ),
          detail = Map(
            "method" -> "POST",
            "authorisationType" -> "user-restricted",
            "requestBody" -> """{"body":"test"}""",
            "apiContext" -> "hello",
            "apiVersion" -> "1.0",
            "Authorization" -> "Bearer accessToken",
            "applicationProductionClientId" -> "applicationClientId",
            "userOID" -> "userOid"
          ),
          generatedAt = requestTimestamp
        ),
        response = DataCall(
          tags = Map(),
          detail = Map(
            "statusCode" -> Status.OK.toString,
            "responseMessage" -> "responseBody"
          ),
          generatedAt = DateTime.now()
        )
      )
    }

    "truncate the request and response to auditBodySizeLimit" in new Setup {
      when(configuration.getInt("auditBodySizeLimit")).thenReturn(Some(5))
      val auditService = new AuditService(configuration, auditConnector, materializer)

      val request = FakeRequest("POST", "/hello/user")
        .withBody(AnyContentAsText("requestBody"))

      val result = await(auditService.auditSuccessfulRequest(request, Default.Ok("responseBody")))

      verify(auditConnector).sendMergedEvent(captor.capture())(any(), any())
      val auditedEvent = captor.getValue.asInstanceOf[MergedDataEvent]

      auditedEvent.request.detail.get("requestBody") shouldBe Some("reque")
      auditedEvent.response.detail.get("responseMessage") shouldBe Some("respo")
    }
  }
}
