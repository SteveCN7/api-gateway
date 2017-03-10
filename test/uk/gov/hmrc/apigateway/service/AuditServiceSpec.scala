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
import uk.gov.hmrc.apigateway.model.{ApiIdentifier, ApiRequest, AuthType}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.{DataCall, DataEvent, MergedDataEvent}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.apigateway.util.HttpHeaders.X_REQUEST_ID
import uk.gov.hmrc.play.test.UnitSpec

class AuditServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  trait Setup {
    implicit val hc = HeaderCarrier()
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val requestId = UUID.randomUUID().toString

    val configuration = mock[Configuration]
    when(configuration.getInt("auditBodySizeLimit")).thenReturn(Some(100))

    val auditConnector = mock[MicroserviceAuditConnector]
    when(auditConnector.sendMergedEvent(any())(any(), any())).thenReturn(AuditResult.Success)
    when(auditConnector.sendEvent(any())(any(), any())).thenReturn(AuditResult.Failure("errorMsg", None))

    val auditService = new AuditService(configuration, auditConnector, materializer)
  }

  override def beforeEach() = {
    setCurrentMillisFixed(10000)
  }

  override def afterEach() = {
    setCurrentMillisSystem()
  }

  "auditSuccessful" should {
    val captor = ArgumentCaptor.forClass(classOf[MergedDataEvent])
    val requestMillis = System.currentTimeMillis()

    "send an audit event" in new Setup {

      val request = FakeRequest("POST", "/api-gateway/hello/user")
        .withBody(AnyContentAsJson(Json.parse("""{"body":"test"}""")))
        .copyFakeRequest(remoteAddress = "10.10.10.10")

      val apiRequest = ApiRequest(
        timeInMillis = Some(requestMillis),
        apiIdentifier = ApiIdentifier("hello", "1.0"),
        authType = AuthType.USER,
        apiEndpoint = "/hello/user",
        userOid = Some("userOid"),
        clientId  = Some("applicationClientId"),
        bearerToken = Some("Bearer accessToken"))

      val result = await(auditService.auditSuccessfulRequest(request, apiRequest, Default.Ok("responseBody"))(requestId))

      verify(auditConnector).sendMergedEvent(captor.capture())(any(), any())
      val auditedEvent = captor.getValue.asInstanceOf[MergedDataEvent]

      auditedEvent shouldBe MergedDataEvent(
        auditSource = "api-gateway",
        auditType = "APIGatewayRequestCompleted",
        eventId = auditedEvent.eventId,
        request = DataCall(
          tags = Map(
            X_REQUEST_ID -> requestId,
            "path" -> "/hello/user",
            "transactionName" -> "Request has been completed via the API Gateway",
            "clientIP" -> "10.10.10.10",
            "clientPort" -> "443",
            "type" -> "Audit"),
          detail = Map(
            "method" -> "POST",
            "authorisationType" -> "user-restricted",
            "requestBody" -> """{"body":"test"}""",
            "apiContext" -> "hello",
            "apiVersion" -> "1.0",
            "Authorization" -> "Bearer accessToken",
            "applicationProductionClientId" -> "applicationClientId",
            "userOID" -> "userOid"),
          generatedAt = new DateTime(requestMillis)
        ),
        response = DataCall(
          tags = Map(),
          detail = Map(
            "statusCode" -> Status.OK.toString,
            "responseMessage" -> "responseBody"
          ),
          generatedAt = DateTime.now())
      )
    }

    "truncate the request and response to auditBodySizeLimit" in new Setup {
      when(configuration.getInt("auditBodySizeLimit")).thenReturn(Some(5))
      val service = new AuditService(configuration, auditConnector, materializer)

      val request = FakeRequest("POST", "/hello/user")
        .withBody(AnyContentAsText("requestBody"))

      val apiRequest = ApiRequest(
        apiIdentifier = ApiIdentifier("hello", "1.0"),
        authType = AuthType.USER,
        apiEndpoint = "/hello/user")

      val result = await(service.auditSuccessfulRequest(request, apiRequest, Default.Ok("responseBody"))(requestId))

      verify(auditConnector).sendMergedEvent(captor.capture())(any(), any())
      val auditedEvent = captor.getValue.asInstanceOf[MergedDataEvent]

      auditedEvent.request.detail.get("requestBody") shouldBe Some("reque")
      auditedEvent.response.detail.get("responseMessage") shouldBe Some("respo")
    }

  }

  "auditFailingRequest" should {

    val captor = ArgumentCaptor.forClass(classOf[DataEvent])
    val requestMillis = System.currentTimeMillis()

    val request = FakeRequest("POST", "/api-gateway/hello/user")
      .withBody(AnyContentAsJson(Json.parse("""{"body":"test"}""")))
      .copyFakeRequest(remoteAddress = "10.10.10.10")

    val apiRequest = ApiRequest(
      timeInMillis = Some(requestMillis),
      apiIdentifier = ApiIdentifier("hello", "1.0"),
      authType = AuthType.USER,
      apiEndpoint = "/hello/user",
      userOid = Some("userOid"),
      clientId = Some("applicationClientId"),
      bearerToken = Some("Bearer accessToken"))

    "send an audit event" in new Setup {

      val result = await(auditService.auditFailingRequest(request, apiRequest, DateTime.parse("2017-02-02"))(requestId))

      verify(auditConnector).sendEvent(captor.capture())(any(), any())
      val auditedEvent = captor.getValue.asInstanceOf[DataEvent]

      auditedEvent shouldBe DataEvent(
        auditSource = "api-gateway",
        auditType = "APIGatewayRequestFailedDueToInvalidAuthorisation",
        eventId = auditedEvent.eventId,
        tags = Map(
          X_REQUEST_ID -> requestId,
          "path" -> "/hello/user",
          "transactionName" -> "A third-party application has made an request rejected by the API Gateway as unauthorised",
          "clientIP" -> "10.10.10.10",
          "clientPort" -> "443",
          "type" -> "Error",
          "generatedAt" -> DateTime.parse("2017-02-02").toString
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
        generatedAt = new DateTime(requestMillis)
      )
    }

  }

}
