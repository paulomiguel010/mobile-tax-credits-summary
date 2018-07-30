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

import play.api.libs.json.Json.toJson
import play.api.libs.ws.{WSRequest, WSResponse}
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.{ShutteredPaymentSummary, TaxCreditsSummary, TaxCreditsSummaryResponse}
import uk.gov.hmrc.mobiletaxcreditssummary.support.BaseISpec

class SandboxTaxCreditsSummaryISpec extends BaseISpec with FileResource {

  private val mobileHeader = "X-MOBILE-USER-ID" -> "208606423740"

  "GET /sandbox/income/:nino/tax-credits/tax-credits-summary " should {
    def request(nino: Nino): WSRequest = wsUrl(s"/income/${nino.value}/tax-credits/tax-credits-summary").withHeaders(acceptJsonHeader)

    def assertEmptyTaxCreditsSummary(response: WSResponse): RuntimeException =
      intercept[RuntimeException] {
        (response.json \ "taxCreditsSummary").get
      }

    "return excluded = false and a tax credit summary where no SANDBOX-CONTROL header is set" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader).get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency").as[String] shouldBe "weekly"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "personalDetails" \ "forename").as[String] shouldBe "Nuala"
    }

    "return excluded = false and a tax credit summary where SANDBOX-CONTROL is any other value" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader, "SANDBOX-CONTROL" -> "RANDOMVALUE").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency").as[String] shouldBe "weekly"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "personalDetails" \ "forename").as[String] shouldBe "Nuala"
    }

    "return excluded = false and a tax credit summary with no claimants section where SANDBOX-CONTROL is CLAIMANTS_FAILURE" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader, "SANDBOX-CONTROL" -> "CLAIMANTS_FAILURE").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency").as[String] shouldBe "weekly"
      (response.json \ "taxCreditsSummary" \ "claimants").toOption shouldBe None
    }

    "return excluded = true and no tax credit summary data if excluded where SANDBOX-CONTROL is EXCLUDED-TAX-CREDITS-USER" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader, "SANDBOX-CONTROL" -> "EXCLUDED-TAX-CREDITS-USER").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe true
      assertEmptyTaxCreditsSummary(response)
    }

    "return excluded = false and no tax credit summary data if non tax credit user where SANDBOX-CONTROL is NON-TAX-CREDITS-USER" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader, "SANDBOX-CONTROL" -> "NON-TAX-CREDITS-USER").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      assertEmptyTaxCreditsSummary(response)
    }

    "return 401 if unauthenticated where SANDBOX-CONTROL is ERROR-401" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader, "SANDBOX-CONTROL" -> "ERROR-401").get())
      response.status shouldBe 401
    }

    "return 403 if forbidden where SANDBOX-CONTROL is ERROR-403" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader, "SANDBOX-CONTROL" -> "ERROR-403").get())
      response.status shouldBe 403
    }

    "return 500 if there is an error where SANDBOX-CONTROL is ERROR-500" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader, "SANDBOX-CONTROL" -> "ERROR-500").get())
      response.status shouldBe 500
    }

    "return 503 and a shutter response where SANDBOX-CONTROL is SHUTTERED" in {
      val response = await(request(sandboxNino).withHeaders(mobileHeader, "SANDBOX-CONTROL" -> "SHUTTERED").get())

      response.status shouldBe 200

      response.json shouldBe toJson(
        TaxCreditsSummaryResponse(
          excluded = false,
          Some(TaxCreditsSummary(
            ShutteredPaymentSummary(
              Some("Your tax credits payment information is currently unavailable. You can still renew your tax credits using the button above.")),
            None))))
    }
  }
}
