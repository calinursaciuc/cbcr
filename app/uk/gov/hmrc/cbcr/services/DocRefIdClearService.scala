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

package uk.gov.hmrc.cbcr.services

import cats.data.EitherT
import cats.instances.all._
import cats.syntax.all._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.cbcr.config.DocRefIdConfig
import uk.gov.hmrc.cbcr.models.DocRefId
import uk.gov.hmrc.cbcr.repositories.{DocRefIdRepository, ReportingEntityDataRepo}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class DocRefIdClearService @Inject()(docRefIdRepo: DocRefIdRepository,
                                     reportingEntityDataRepo: ReportingEntityDataRepo,
                                     audit: AuditConnector,
                                     docRefIdConfig: DocRefIdConfig)(implicit ec: ExecutionContext) {

  private val DOCREFID_AUDIT = "CBCR-DocRefIdClear"

  val docRefIds: List[DocRefId] = docRefIdConfig.docRefIdClear.split("_").toList.map(DocRefId.apply)

  if (docRefIds.nonEmpty) {
    Logger.info(s"About to clear DocRefIds:\n${docRefIds.mkString("\n")}")
    docRefIds.map { d =>
      (for {
        _ <- EitherT.right(docRefIdRepo.delete(d))
        _ <- EitherT.right(reportingEntityDataRepo.delete(d))
        _ <- auditDocRefIdClear(d)
      } yield ()).value
    }.sequence[Future, Either[String, Unit]].map(_.separate._1.foreach(Logger.error(_))).onComplete {
      case Success(_) => Logger.info(s"Successfully deleted and audited ${docRefIds.size} DocRefIds")
      case Failure(t) => Logger.error(s"Error in deleting and auditing the docRefIds: ${t.getMessage}", t)
    }
  }


  private def auditDocRefIdClear(docRefId: DocRefId): EitherT[Future, String, Unit] = {
    EitherT[Future, String, Unit](
      audit.sendExtendedEvent(ExtendedDataEvent("Country-By-Country-Backend", DOCREFID_AUDIT,
        detail = Json.obj(
          "docRefId" -> Json.toJson(docRefId)
        )
      )).map {
        case AuditResult.Success => Right(())
        case AuditResult.Failure(msg, _) => Left(s"failed to audit: $msg")
        case AuditResult.Disabled => Right(())
      })
  }
}
