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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.test.FakeRequest
import uk.gov.hmrc.cbcr.WireMockResponses.AuthResponses
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.config.CbcrIdConfig
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.services.{LocalSubscription, RemoteSubscription, SubscriptionHandlerImpl}
import uk.gov.hmrc.cbcr.testsupport.ItSpec
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier


class CBCIdControllerSpec extends ItSpec with ScalaFutures with MockitoSugar with BeforeAndAfterEach {

  val localGen = mock[LocalSubscription]
  val remoteGen = mock[RemoteSubscription]

  implicit val as = app.injector.instanceOf[ActorSystem]
  val config = app.injector.instanceOf[Configuration]

  def testConfig(desApi: Boolean) = CbcrIdConfig(true, true, 60, true, useDESApi = desApi, true, "", true, "", "", "")

  implicit val mat = ActorMaterializer()

  val srb = SubscriptionDetails(
    BusinessPartnerRecord("SafeID",Some(OrganisationResponse("Name")), EtmpAddress("Some ave",None,None,None,None, "GB")),
    SubscriberContact(name = None, "dave", "jones", PhoneNumber("123456789").get,EmailAddress("bob@bob.com")),
    None,
    Utr("7000000002")
  )
  val crb = CorrespondenceDetails(EtmpAddress("Some ave",None,None,None,None, "GB"),
    ContactDetails(EmailAddress("bob@bob.com"),PhoneNumber("123456789").get),
    ContactName("Bob", "Bobbet"))

  val id = CBCId.create(1).getOrElse(fail("Can not generate CBCId"))

  lazy val clientAuth = app.injector.instanceOf[CBCRAuth]
  override def afterEach(): Unit = {
    super.afterEach()
    reset(localGen,remoteGen)
  }

  "The CBCIdController" - {
    "query the localCBCId generator when useDESApi is set to false" in {
AuthResponses.authorisedResponse()
      val handler = new SubscriptionHandlerImpl(testConfig(false),localGen,remoteGen)
      val controller = new CBCIdController(handler,clientAuth, cc)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.toJson(srb))
      when(localGen.createSubscription(any())(any())) thenReturn Future.successful(Ok(Json.obj("cbc-id" -> id.value)))
      val response = controller.subscribe()(fakeRequestSubscribe).futureValue
      response.header.status shouldBe Status.OK
      jsonBodyOf(response) shouldEqual Json.obj("cbc-id" -> "XTCBC0100000001")
    }
    "query the remoteCBCId generator when useDESApi is set to true" in {
      AuthResponses.authorisedResponse()
      val handler = new SubscriptionHandlerImpl(testConfig(true),localGen,remoteGen)
      val controller = new CBCIdController(handler,clientAuth, cc)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.toJson(srb))
      when(remoteGen.createSubscription(any())(any())) thenReturn Future.successful(Ok(Json.obj("cbc-id" -> id.value)))
      val response = controller.subscribe()(fakeRequestSubscribe)
     status(response) shouldBe Status.OK
      jsonBodyOf(response).futureValue shouldEqual Json.obj("cbc-id" -> "XTCBC0100000001")
    }
    "generate bad request response if request doesn't contain valid subscriptionDetails" in {
AuthResponses.authorisedResponse()
      val handler = new SubscriptionHandlerImpl(testConfig(false),localGen,remoteGen)
      val controller = new CBCIdController(handler,clientAuth, cc)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.obj("bad" -> "request"))
      val response = controller.subscribe()(fakeRequestSubscribe)
      status(response) shouldBe Status.BAD_REQUEST
    }
    "return 200 when updateSubscription passed valid CorrespondenceDetails in request" in {
AuthResponses.authorisedResponse()
      val handler = new SubscriptionHandlerImpl(testConfig(true),localGen,remoteGen)
      val controller = new CBCIdController(handler,clientAuth, cc)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.toJson(crb))
      when(remoteGen.updateSubscription(any(),any())(any())) thenReturn Future.successful(Ok(Json.obj("cbc-id" -> id.value)))
      val response = controller.updateSubscription("safeId")(fakeRequestSubscribe)
      status(response) shouldBe Status.OK
    }
    "return 400 when updateSubscription passed invalid CorrespondenceDetails in request" in {
AuthResponses.authorisedResponse()
      val handler = new SubscriptionHandlerImpl(testConfig(true),localGen,remoteGen)
      val controller = new CBCIdController(handler,clientAuth, cc)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.obj("bad" -> "request"))
      val response = controller.updateSubscription("safeId")(fakeRequestSubscribe)
      status(response) shouldBe Status.BAD_REQUEST
    }

    "no error generated when getSubscription called" in {
AuthResponses.authorisedResponse()
      val handler = new SubscriptionHandlerImpl(testConfig(false),localGen,remoteGen)
      val controller = new CBCIdController(handler,clientAuth, cc)
      val fakeRequestSubscribe = FakeRequest("GET", "/cbc-id")
      when(localGen.getSubscription(any())(any())) thenReturn Future.successful(Ok(Json.obj("some" -> "thing")))
      val response = controller.getSubscription("safeId")(fakeRequestSubscribe)
      status(response) shouldBe Status.OK
    }
  }

}
