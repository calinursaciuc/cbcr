/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr.connectors

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.cbcr.config.DesConfig
import uk.gov.hmrc.cbcr.models.{CorrespondenceDetails, MigrationRequest, SubscriptionRequest}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future, Promise}

class DESConnector @Inject()(val ec: ExecutionContext,
                             val auditConnector: AuditConnector,
                             val http: HttpClient,
                             val desConfig: DesConfig) extends RawResponseReads {
  //with RawResponseReads {

  lazy val serviceUrl: String = desConfig.serviceUrl
  lazy val orgLookupURI: String = "registration/organisation"
  lazy val cbcSubscribeURI: String = "country-by-country/subscription"
  lazy val urlHeaderEnvironment: String = desConfig.urlHeaderEnvironment
  lazy val urlHeaderAuthorization: String = desConfig.authorisationToken

  val audit = new Audit("known-fact-checking", auditConnector)

  val lookupData: JsObject = Json.obj(
    "regime" -> "ITSA",
    "requiresNameMatch" -> false,
    "isAnAgent" -> false
  )

  val stubMigration: Boolean = desConfig.stubMigration

  val delayMigration: Int = 1000 * desConfig.delayMigration

  private def createHeaderCarrier: HeaderCarrier =
    HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))

  def lookup(utr: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    Logger.info(s"Lookup Request sent to DES: POST $serviceUrl/$orgLookupURI/utr/$utr")
    http.POST[JsValue, HttpResponse](s"$serviceUrl/$orgLookupURI/utr/$utr", Json.toJson(lookupData)).recover {
      case e: HttpException => HttpResponse(e.responseCode, responseString = Some(e.message))
    }
  }

  def createSubscription(sub: SubscriptionRequest): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    implicit val writes = SubscriptionRequest.subscriptionWriter
    Logger.info(s"Create Request sent to DES: ${Json.toJson(sub)} for safeID: ${sub.safeId}")
    http.POST[SubscriptionRequest, HttpResponse](s"$serviceUrl/$cbcSubscribeURI", sub).recover {
      case e: HttpException => HttpResponse(e.responseCode, responseString = Some(e.message))
    }
  }

  def createMigration(mig: MigrationRequest): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    implicit val writes = MigrationRequest.migrationWriter
    Logger.info(s"Migration Request sent to DES for safeId: ${mig.safeId} and CBCId: ${mig.cBCId}")

    Logger.warn(s"stubMigration set to: $stubMigration")
    val res = Promise[HttpResponse]()
    Future {
      if (!stubMigration) {
        Logger.info("calling ETMP for migration")
        Thread.sleep(delayMigration)
        http.POST[MigrationRequest, HttpResponse](s"$serviceUrl/$cbcSubscribeURI", mig).recover {
          case e: HttpException => HttpResponse(e.responseCode, responseString = Some(e.message))
        }.map(r => {
          Logger.info(s"Migration Status for safeId: ${mig.safeId} and cBCId: ${mig.cBCId} ${r.status}")
          res.success(r)
        })
      } else {
        Logger.info("in migration stub")

        Thread.sleep(delayMigration)
        res.success(HttpResponse(200, responseString = Some(s"migrated ${mig.cBCId}")))
      }
    }
    res.future
  }

  def updateSubscription(safeId: String, cor: CorrespondenceDetails): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    implicit val format = CorrespondenceDetails.updateWriter
    Logger.info(s"Update Request sent to DES: $cor for safeID: $safeId")
    http.PUT[CorrespondenceDetails, HttpResponse](s"$serviceUrl/$cbcSubscribeURI/$safeId", cor).recover {
      case e: HttpException => HttpResponse(e.responseCode, responseString = Some(e.message))
    }
  }

  def getSubscription(safeId: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    Logger.info(s"Get Request sent to DES for safeID: $safeId")
    http.GET[HttpResponse](s"$serviceUrl/$cbcSubscribeURI/$safeId").recover {
      case e: HttpException => HttpResponse(e.responseCode, responseString = Some(e.message))
    }
  }

}
