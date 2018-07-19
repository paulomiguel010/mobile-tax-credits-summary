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

package uk.gov.hmrc.mobiletaxcreditssummary

import play.api.libs.json.Json.parse
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.AuthStub.grantAccess
import uk.gov.hmrc.mobiletaxcreditssummary.support.BaseISpec

class ShutteringISpec extends BaseISpec with FileResource {
  val message = "Shuttering_message."

  override def config: Map[String, Any] = super.config ++ Map(
    "microservice.services.tax-credits-broker.shuttered" -> true,
    "microservice.services.tax-credits-broker.shutteredMessage" -> message
  )

  "GET /income/:nino/tax-credits/tax-credits-summary " should {
    "return a shuttered payload" in {
      grantAccess(nino1.value)

      val response = await(wsUrl(s"/income/${nino1.value}/tax-credits/tax-credits-summary").withHeaders(acceptJsonHeader).get())

      response.status shouldBe 200
      response.json shouldBe parse(
        """
          {
            "excluded":false,
            "taxCreditsSummary":{
              "paymentSummary":{
                "workingTaxCredit":{
                  "paymentSeq":[],
                  "paymentFrequency":"weekly"},
                "childTaxCredit":{
                  "paymentSeq":[],
                  "paymentFrequency":"weekly"},
                "paymentEnabled":true,
                "informationMessage":"Shuttering message.",
                "totalsByDate":[]
              }
            }
          } """.stripMargin)
    }
  }

}
