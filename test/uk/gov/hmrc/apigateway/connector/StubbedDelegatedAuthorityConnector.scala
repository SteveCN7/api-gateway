/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.apigateway.connector

import org.joda.time.DateTime
import play.api.libs.json.Json._
import uk.gov.hmrc.apigateway.connector.impl.DelegatedAuthorityConnector
import uk.gov.hmrc.apigateway.exception.GatewayError.NotFound
import uk.gov.hmrc.apigateway.model.{Authority, ThirdPartyDelegatedAuthority, Token}
import uk.gov.hmrc.apigateway.play.binding.PlayBindings.authorityFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source._

class StubbedDelegatedAuthorityConnector extends DelegatedAuthorityConnector(null) {

  override def getByAccessToken(accessToken: String): Future[Authority] = Future {
    loadStubbedDelegatedAuthority(accessToken)
  }

  private def loadStubbedDelegatedAuthority(accessToken: String): Authority = {
    val inputStream = getClass.getResourceAsStream(s"/stub/authority/$accessToken.json")
    Option(inputStream).map(fromInputStream).map(_.mkString).map(parse(_).as[Authority]) match {
      case Some(authority) =>
        if (authority.authExpired) authority
        else {
          val validToken: Token = authority.delegatedAuthority.token.copy(expiresAt = DateTime.now().plusMinutes(5))
          val validTpda: ThirdPartyDelegatedAuthority = authority.delegatedAuthority.copy(token = validToken)
          authority.copy(delegatedAuthority = validTpda)
        }
      case _ => throw NotFound()
    }
  }

}
