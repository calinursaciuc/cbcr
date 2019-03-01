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

package uk.gov.hmrc.cbcr.WireMockResponses

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object AuthResponses {

  def unauthenticatedResponse(): StubMapping = {
    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withBody("")
            .withHeader("WWW-Authenticate", "MDTP detail=\"MissingBearerToken\"")
            .withHeader("Date", "Wed, 01 Aug 2018 07:40:11 GMT")
            .withHeader("Content-Length", "0"))
    )
  }

  def unauthorisedResponse(): StubMapping = {
    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              """{
                |"allEnrolments":
                |  [
                |   {
                |    "key":"some_rubbish",
                |    "identifiers":[],
                |    "state":"activated"
                |   }
                |  ],
                |  "credentials":
                |     {
                |      "providerId":"123",
                |      "providerType":"PrivilegedApplication"
                |     }
                |}""".stripMargin)
        ))
  }

  def authorisedResponse(): StubMapping = {
    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              """{"affinityGroup":"Agent"}"""
             .stripMargin)
        ))
  }

}
