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

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.cbcr.config.RetrieveConfig
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import scala.concurrent.ExecutionContext

class RetrieveReportingEntityService @Inject()(repo: ReportingEntityDataRepo,
                                               audit: AuditConnector,
                                               retrieveConfig: RetrieveConfig
                                              )(implicit ex: ExecutionContext) {

  val retrieveReportingEntity: Boolean = retrieveConfig.reportingEntity
  Logger.info(s"retrieveReportingEntity set to: $retrieveReportingEntity")

  if (retrieveReportingEntity) {
    val docRefId: String = retrieveConfig.docRefId
    Logger.info(s"docRefId to retireve = ${docRefId}")

    repo.query(docRefId).map(red =>
      if (red.size > 0) red.foreach(r => Logger.info(s"reportingEntityData for docRefId ${docRefId} = ${Json.toJson(r)}"))
      else Logger.info(s"no reportingEntityData found for docRefIds matching $docRefId")
    )
  }
}
