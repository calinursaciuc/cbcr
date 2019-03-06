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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.{FakeRequest, Helpers}
import reactivemongo.api.commands.{DefaultWriteResult, WriteError}
import uk.gov.hmrc.cbcr.WireMockResponses.AuthResponses
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.FileUploadRepository
import uk.gov.hmrc.cbcr.testsupport.ItSpec

import scala.concurrent.Future

class FileUploadResponseControllerSpec extends ItSpec {

  val fir = UploadFileResponse("id1", "fid1", "status", None)

  val okResult = DefaultWriteResult(true, 0, Seq.empty, None, None, None)

  val failResult = DefaultWriteResult(false, 1, Seq(WriteError(1, 1, "Error")), None, None, Some("Error"))

  val fakePostRequest: FakeRequest[JsValue] = FakeRequest(Helpers.POST, "/saveFileUploadResponse").withBody(toJson(fir))

  val badFakePostRequest: FakeRequest[JsValue] = FakeRequest(Helpers.POST, "/saveFileUploadResponse").withBody(Json.obj("bad" -> "request"))

  val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(Helpers.GET, "/retrieveFileUploadResponse")

  implicit val as = ActorSystem()
  implicit val mat = ActorMaterializer()

  val repo = mock[FileUploadRepository]
  lazy val clientAuth = app.injector.instanceOf[CBCRAuth]

  val uploadUrl = "http://localhost:19001/cbcr/file-upload-response"

  val controller = new FileUploadResponseController(repo, clientAuth, cc)

  "The FileUploadResponseController" - {
    "respond with a 200 when asked to store an UploadFileResponse" in {
      AuthResponses.authorisedResponse()
      when(repo.save(any(classOf[UploadFileResponse]))).thenReturn(Future.successful(okResult))
      val result = httpClient.POST(uploadUrl, Json.toJson(fir)).futureValue
      result.status shouldBe Status.OK
    }

    "respond with a 500 if there is a DB failure" in {
      when(repo.save(any(classOf[UploadFileResponse]))).thenReturn(Future.failed(new RuntimeException("")))
      val result = controller.saveFileUploadResponse(fakePostRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "respond with a 400 if UploadFileResponse in request is invalid" in {
      AuthResponses.authorisedResponse()
      when(repo.save(any(classOf[UploadFileResponse]))).thenReturn(Future.successful(failResult))
      val result = httpClient.POST(uploadUrl, Json.obj("bad" -> "request")).futureValue
      result.status shouldBe Status.BAD_REQUEST
    }

    "respond with a 200 and a FileUploadResponse when asked to retrieve an existing envelopeId" in {
      AuthResponses.authorisedResponse()
      when(repo.get(any(classOf[String]))).thenReturn(Future.successful(Some(fir)))
      val result = controller.retrieveFileUploadResponse("envelopeIdOk")(fakeGetRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(result).validate[UploadFileResponse].isSuccess shouldBe true
    }

    "respond with a 204 when asked to retrieve a non-existent envelopeId" in {
      AuthResponses.authorisedResponse()
      when(repo.get(any(classOf[String]))).thenReturn(Future.successful(None))
      val result = controller.retrieveFileUploadResponse("envelopeIdFail")(fakeGetRequest)
      status(result) shouldBe Status.NO_CONTENT
    }

  }

}
