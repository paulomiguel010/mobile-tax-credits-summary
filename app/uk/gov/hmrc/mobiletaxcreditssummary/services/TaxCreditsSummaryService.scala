/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.mobiletaxcreditssummary.services

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.api.service._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors._
import uk.gov.hmrc.mobiletaxcreditssummary.domain._
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ExecutionContext, Future}

trait TaxCreditsSummaryService {
  def getTaxCreditsSummaryResponse(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[TaxCreditsSummaryResponse]
}

@Singleton
class LiveTaxCreditsSummaryService @Inject()(taxCreditsBrokerConnector: TaxCreditsBrokerConnector,
                                             val auditConnector: AuditConnector,
                                             val appNameConfiguration: Configuration) extends TaxCreditsSummaryService with Auditor {

  override def getTaxCreditsSummaryResponse(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[TaxCreditsSummaryResponse] = {
    val tcNino = TaxCreditsNino(nino.value)

    def buildTaxCreditsSummary(paymentSummary: PaymentSummary): Future[TaxCreditsSummaryResponse] = {
      def getChildrenAge16AndUnder: Future[Seq[Person]] = {
        taxCreditsBrokerConnector.getChildren(tcNino).map(children =>
          Child.getEligibleChildren(children))
      }

      val childrenFuture = getChildrenAge16AndUnder
      val partnerDetailsFuture = taxCreditsBrokerConnector.getPartnerDetails(tcNino)
      val personalDetailsFuture = taxCreditsBrokerConnector.getPersonalDetails(tcNino)

      val claimants: Future[Option[Claimants]] = (for {
        children <- childrenFuture
        partnerDetails <- partnerDetailsFuture
        personalDetails <- personalDetailsFuture
      } yield Some(Claimants(personalDetails, partnerDetails, children))).recover {
        case _ => None
      }

      claimants.map(c => TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, c))))

    }

    def buildResponseFromPaymentSummary: Future[TaxCreditsSummaryResponse] = {
      withAudit("getTaxCreditSummary", Map("nino" -> nino.value)) {
        taxCreditsBrokerConnector.getPaymentSummary(tcNino).flatMap { summary =>
          if (summary.excluded.getOrElse(false)) {
            // in the context of getPaymentSummary, 'excluded == true' means a non-tax credits user
            Future successful TaxCreditsSummaryResponse(excluded = false, None)
          }
          else {
            buildTaxCreditsSummary(summary)
          }
        }
      }
    }

    def exclusion: Future[TaxCreditsSummaryResponse] = {
      withAudit("getTaxCreditExclusion", Map("nino" -> nino.value)) {
        taxCreditsBrokerConnector.getExclusion(tcNino).map(
          exclusion =>
            if (exclusion.excluded) TaxCreditsSummaryResponse(excluded = true, None)
            else throw new IllegalStateException("payment summary call failed but user not excluded")
        )
      }
    }

    buildResponseFromPaymentSummary.recoverWith {
      case _ => exclusion
    }
  }
}
