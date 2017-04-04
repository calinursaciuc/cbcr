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

package uk.gov.hmrc.cbcr

import cats.data.EitherT
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.cbcr.core.ServiceResponse
import uk.gov.hmrc.cbcr.models.{DbOperationResult, InvalidState, UpdateSuccess}

import scala.concurrent.{ExecutionContext, Future}

package object repositories {

  def checkUpdateResult(future: Future[WriteResult])(implicit ex: ExecutionContext): ServiceResponse[DbOperationResult] =
    EitherT(future.map { r =>
      if (r.ok) Right(UpdateSuccess)
      else Left(InvalidState(r.errmsg.getOrElse("Update failed")))
    }.recover {
      case t: Throwable => Left(InvalidState(t.getMessage))
    })
}
