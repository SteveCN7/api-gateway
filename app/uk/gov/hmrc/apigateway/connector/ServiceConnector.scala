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

package uk.gov.hmrc.apigateway.connector

import play.api.libs.json.Format
import play.api.libs.ws.WSClient
import uk.gov.hmrc.apigateway.cache.CacheManager
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class ServiceConnector(wsClient: WSClient, cache: CacheManager, val serviceName: String)
  extends AbstractConnector(wsClient) with ServicesConfig {

  lazy val serviceBaseUrl = baseUrl(serviceName)

  override def get[T: ClassTag](urlPath: String)(implicit format: Format[T]): Future[T] =
    get(urlPath, urlPath, Seq.empty)

  def get[T: ClassTag](key: String, urlPath: String)(implicit format: Format[T]): Future[T] =
    get(key, urlPath, Seq.empty)

  def get[T: ClassTag](key: String, urlPath: String, headers: Seq[(String, String)])(implicit format: Format[T]): Future[T] =
    cache.get[T](key, serviceName, super.get(s"$serviceBaseUrl/$urlPath", headers))
}
