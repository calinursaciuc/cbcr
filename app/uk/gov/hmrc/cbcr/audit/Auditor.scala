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

package audit

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class Auditor @Inject()(val auditConnector: AuditConnector) {

  def sendEvent(event: DataEvent)(implicit hc: HeaderCarrier): Future[Unit] = {
    auditConnector
      .sendEvent(event)
      .map(_ => ())
      .recover {
        case NonFatal(e) => Logger.warn(s"Unable to post audit event of type ${event.auditType} to audit connector - ${e.getMessage}", e)
      }
  }
}