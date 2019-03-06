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

package uk.gov.hmrc.cbcr.config

import com.google.inject.Inject
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

case class DesConfig(serviceUrl: String,
                     urlHeaderEnvironment: String,
                     authorisationToken: String,
                     stubMigration: Boolean,
                     delayMigration: Int
                    ) {

  @Inject
  def this(servicesConfig: ServicesConfig) = {
    this(
      serviceUrl            = servicesConfig.baseUrl("etmp-hod"),
      urlHeaderEnvironment = servicesConfig.getString("microservice.services.etmp-hod.environment"),
      authorisationToken = servicesConfig.getString("microservice.services.etmp-hod.authorization-token"),
      stubMigration = servicesConfig.getBoolean(s"CBCId.stubMigration"),
      delayMigration = servicesConfig.getInt(s"CBCId.delayMigration")
    )
  }
}

object DesConfig {
  implicit val desConfigFormat: Format[DesConfig] = Json.format[DesConfig]
}
