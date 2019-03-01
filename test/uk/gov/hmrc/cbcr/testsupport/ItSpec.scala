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

package uk.gov.hmrc.cbcr.testsupport

import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import org.joda.time.Seconds
import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, DurationConversions, FiniteDuration}

trait ItSpec extends FreeSpec
  with WireMockSupport
  with RichMatchers
  with GuiceOneServerPerSuite {

  protected lazy val baseUrl = s"http://localhost:$port"

  implicit lazy val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc = HeaderCarrier()

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(
    "microservice.services.auth.port" -> WireMockSupport.port
  ).build()

  def injector: Injector = app.injector

  lazy val httpClient: HttpClient = new HttpClient with WSHttp {
    override def wsClient: WSClient = injector.instanceOf(classOf[WSClient])

    override val hooks: Seq[HttpHook] = Nil
    override val configuration = None

    override protected def actorSystem = injector.instanceOf(classOf[ActorSystem])
  }

  implicit val defaultTimeout: FiniteDuration = FiniteDuration(5, TimeUnit.SECONDS)

  implicit def extractAwait[A](future: Future[A]): A = await[A](future)

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  // Convenience to avoid having to wrap andThen() parameters in Future.successful
  implicit def liftFuture[A](v: A): Future[A] = Future.successful(v)

  def status(of: Result): Int = of.header.status

  def status(of: Future[Result])(implicit timeout: Duration): Int = status(Await.result(of, timeout))

  def jsonBodyOf(result: Result)(implicit mat: Materializer): JsValue = {
    Json.parse(bodyOf(result))
  }

  def jsonBodyOf(resultF: Future[Result])(implicit mat: Materializer): Future[JsValue] = {
    resultF.map(jsonBodyOf)
  }

  def bodyOf(result: Result)(implicit mat: Materializer): String = {
    val bodyBytes: ByteString = await(result.body.consumeData)
    // We use the default charset to preserve the behaviour of a previous
    // version of this code, which used new String(Array[Byte]).
    // If the fact that the previous version used the default charset was an
    // accident then it may be better to decode in UTF-8 or the charset
    // specified by the result's headers.
    bodyBytes.decodeString(Charset.defaultCharset().name)
  }

  def bodyOf(resultF: Future[Result])(implicit mat: Materializer): Future[String] = {
    resultF.map(bodyOf)
  }

}
