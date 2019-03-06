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

package uk.gov.hmrc.cbcr.controllers

import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.cbcr.WireMockResponses.{AuthResponses, EmailResponses}
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.services.EmailService
import uk.gov.hmrc.cbcr.testsupport.ItSpec

class CBCREmailControllerSpec extends ItSpec {

  val paramsSub = Map("f_name" → "Tyrion", "s_name" → "Lannister", "cbcrId" -> "XGCBC0000000001")
  val correctEmail: Email = Email(List("tyrion.lannister@gmail.com"), "cbcr_subscription", paramsSub)
  val mockEmailService = app.injector.instanceOf[EmailService]
  val clientAuth = app.injector.instanceOf[CBCRAuth]
  val cc = mock[ControllerComponents]
  val cbcrEmailController = new CBCREmailController(mockEmailService, clientAuth, cc)

  val emailUrl = s"http://localhost:19001/cbcr/email"
  val emailJson = Email
  "The CBCREmailController" - {
    "return a 202 for a valid rest call" in {
      AuthResponses.authorisedResponse()
      EmailResponses.send(true)
      val response = httpClient.POST(emailUrl, Json.toJson(correctEmail)).futureValue
      response.status shouldBe Status.ACCEPTED
    }
    "return a 400 for a call with invalid email" in {
      AuthResponses.authorisedResponse()
      EmailResponses.send(false)

      val response = the[Exception] thrownBy (httpClient.POST(emailUrl, Json.obj("bad" -> "request")).futureValue)
      response.getMessage should startWith("The future returned an exception of type: uk.gov.hmrc.http.BadRequestException, with message: POST of 'http://localhost:19001/cbcr/email' returned 400 (Bad Request)")

    }
  }
}
