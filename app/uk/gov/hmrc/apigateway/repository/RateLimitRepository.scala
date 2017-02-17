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

package uk.gov.hmrc.apigateway.repository

import javax.inject.{Inject, Singleton}

import org.joda.time.LocalDateTime
import org.joda.time.LocalDateTime.now
import play.api.Configuration
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.GetLastError
import reactivemongo.api.commands.GetLastError.WaitForAknowledgments
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.apigateway.exception.GatewayError.ThrottledOut
import uk.gov.hmrc.apigateway.util.Time
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.sequence

case class RateLimitCounter(clientId: String, minutesSinceEpoch: Long, createdAt: LocalDateTime = now(), count: Int = 1)

@Singleton
class RateLimitRepository @Inject()(val reactiveMongoApi: ReactiveMongoApi,
                                    val configuration: Configuration) {

  implicit val format = Json.format[RateLimitCounter]

  val databaseFuture = reactiveMongoApi.database.map(_.collection[JSONCollection]("rateLimitCounter"))
  lazy val mongoWrite = configuration.getInt("mongodb.w").getOrElse(throw new RuntimeException("mongodb.w not set"))
  lazy val mongoJournal = configuration.getBoolean("mongodb.j").getOrElse(throw new RuntimeException("mongodb.j not set"))

  val indexes = Seq(
    Index(
      Seq("clientId" -> IndexType.Ascending, "minutesSinceEpoch" -> IndexType.Ascending),
      name = Some("rateLimitCounterIndex"),
      unique = true,
      background = true),
    Index(
      Seq("createdAt" -> IndexType.Ascending),
      name = Some("rateLimitCounterTTLIndex"),
      options = BSONDocument("expireAfterSeconds" -> 60))
  )

  def validateAndIncrement(clientId: String, threshold: Int): Future[Unit] = {
    val minutesSinceEpoch = Time.minutesSinceEpoch()

    def findRateLimitCounter(rateLimitCounterDb: JSONCollection) = {
      rateLimitCounterDb.find(Json.obj(
        "clientId" -> clientId,
        "minutesSinceEpoch" -> minutesSinceEpoch))
        .one[RateLimitCounter](ReadPreference.nearest)
    }

    def incrementRateLimitCounter(rateLimitCounterDb: JSONCollection) = {
      rateLimitCounterDb.update(
        Json.obj("clientId" -> clientId, "minutesSinceEpoch" -> minutesSinceEpoch),
        Json.obj("$inc" -> Json.obj("count" -> 1), "$setOnInsert" -> Json.obj("createdAt" -> now())),
        upsert = true,
        writeConcern = GetLastError(WaitForAknowledgments(mongoWrite), mongoJournal, fsync = false))
    }

    for {
      db <- databaseFuture
      counter <- findRateLimitCounter(db)
      result <- counter match {
        case Some(r) if r.count >= threshold => Future.failed(new ThrottledOut)
        case _ => incrementRateLimitCounter(db)
      }
    } yield ()
  }

  def ensureIndexes(): Future[Unit] = {
    for {
      db <- databaseFuture
      _ <- sequence(indexes.map(index => db.indexesManager.create(index)))
    } yield ()
  }

  ensureIndexes()
}
