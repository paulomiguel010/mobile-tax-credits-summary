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

package uk.gov.hmrc.mobiletaxcreditssummary.service

import play.api.Configuration
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.mobiletaxcreditssummary.connectors.TaxCreditsBrokerConnector
import uk.gov.hmrc.mobiletaxcreditssummary.controllers.TestSetup
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.mobiletaxcreditssummary.services.LiveTaxCreditsSummaryService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global

class TaxCreditsSummaryServiceSpec extends TestSetup with FileResource with FutureAwaits with DefaultAwaitTimeout {
  implicit val taxCreditsBrokerConnector: TaxCreditsBrokerConnector = mock[TaxCreditsBrokerConnector]
  implicit val auditConnector:            AuditConnector            = mock[AuditConnector]
  val configuration:                      Configuration             = mock[Configuration]

  val service = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector)

  val exclusionPaymentSummary = PaymentSummary(None, None, None, None, excluded = Some(true))
  val taxCreditsNino          = TaxCreditsNino(nino)

  val upstream4xxException = Upstream4xxResponse("blows up for excluded users", 405, 405)
  val upstream5xxException = Upstream5xxResponse("blows up for excluded users", 500, 500)

  val taxCreditsSummary =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, Some(claimants))))

  val taxCreditsSummaryNoPartnerDetails =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, Some(claimantsNoPartnerDetails))))

  val taxCreditsSummaryNoChildren =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, Some(claimantsNoChildren))))

  val taxCreditsSummaryNoClaimants =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, None)))

  "getTaxCreditsSummaryResponse" should {
    "return a non-tax-credits user payload when exclusion returns None" in {
      mockTaxCreditsBrokerConnectorGetExclusion(None, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe TaxCreditsSummaryResponse(excluded = false, None)
    }

    "return a tax-credits user payload when a payment summary is returned" in {
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(paymentSummary, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildren(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummary
    }

    "return a tax-credits user payload when a payment summary is returned but when there are no partner details" in {
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(paymentSummary, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildren(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetails(None, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryNoPartnerDetails
    }

    "return a tax-credits user payload when a payment summary is returned but when there are no children" in {
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(paymentSummary, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildren(Seq.empty, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryNoChildren
    }

    "return an excluded user payload when exclusion returns true" in {
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(true)), taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe TaxCreditsSummaryResponse(excluded = true, None)
    }

    "return TaxCreditsSummaryResponse with payment summary but empty claimants when Get Children fails" in {
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(paymentSummary, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildrenFailure(upstream5xxException, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryNoClaimants
    }

    "return TaxCreditsSummaryResponse with payment summary but empty claimants when Get Personal Details fails" in {
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(paymentSummary, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildren(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetailsFailure(upstream4xxException, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryNoClaimants
    }

    "return TaxCreditsSummaryResponse with payment summary but empty claimants when Get Partner Details fails" in {
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(paymentSummary, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildren(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetailsFailure(upstream5xxException, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryNoClaimants
    }

    "return an error when payment summary fails and exclusion returns false" in {
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentFailure(upstream5xxException, taxCreditsNino)

      intercept[Upstream5xxResponse] {
        await(service.getTaxCreditsSummaryResponse(Nino(nino)))
      }
    }

    "return an error when exclusion errors" in {
      mockTaxCreditsBrokerConnectorGetExclusionFailure(Upstream4xxResponse("blows up for excluded users", 400, 400), taxCreditsNino)

      intercept[Upstream4xxResponse] {
        await(service.getTaxCreditsSummaryResponse(Nino(nino)))
      }
    }
  }
}
