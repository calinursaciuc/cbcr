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

import com.google.inject._
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.config.ServicesConfig

class ConfigModule extends AbstractModule {

  @Provides
  @Singleton
  def provideServiceConfig(environment: Environment, configuration: Configuration): ServicesConfig = new ServicesConfig {
    def mode: Mode = environment.mode

    def runModeConfiguration: Configuration = configuration
  }

  @Provides
  @Singleton
  def authorisedFunctions(ac: AuthConnector): AuthorisedFunctions = new AuthorisedFunctions {
    override def authConnector: AuthConnector = ac
  }

  override def configure(): Unit = ()
}