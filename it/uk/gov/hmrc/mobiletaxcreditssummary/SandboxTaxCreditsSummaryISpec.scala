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

import play.api.libs.ws.{WSRequest, WSResponse}
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobiletaxcreditssummary.support.BaseISpec

class SandboxTaxCreditsSummaryISpec extends BaseISpec with FileResource {

  private val mobileHeader = "X-MOBILE-USER-ID" -> "208606423740"

  "GET /sandbox/income/:nino/tax-credits/tax-credits-summary " should {
    def request(nino: Nino): WSRequest = wsUrl(s"/income/${nino.value}/tax-credits/tax-credits-summary").withHeaders(acceptJsonHeader)

    def assertEmptyTaxCreditSummary(response: WSResponse): RuntimeException =
      intercept[RuntimeException] {
        (response.json \ "taxCreditSummary").get
      }

    "return excluded = false and a tax credit summary" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader).get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency").as[String] shouldBe "weekly"
    }

    "return excluded = true and no tax credit summary data if excluded" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader).get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe true
      assertEmptyTaxCreditSummary(response)
    }

    "return excluded = false and no tax credit summary data if non tax credit user" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader).get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      assertEmptyTaxCreditSummary(response)
    }

    "return 401 if unauthenticated" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader).get())
      response.status shouldBe 401
    }

    "return 403 if forbidden" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader).get())
      response.status shouldBe 403
    }

    "return 500 if there is an error" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader).get())
      response.status shouldBe 500
    }
  }
}
