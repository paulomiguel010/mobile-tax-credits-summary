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
import play.api.libs.json.Json
import uk.gov.hmrc.api.sandbox._
import uk.gov.hmrc.api.service._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors._
import uk.gov.hmrc.mobiletaxcreditssummary.domain._
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ExecutionContext, Future}

trait TaxCreditsSummaryService {

  def getTaxCreditExclusion(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Exclusion]

  def getTaxCreditSummary(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[TaxCreditSummary]
}

@Singleton
class LiveTaxCreditsSummaryService @Inject()(taxCreditsBrokerConnector: TaxCreditsBrokerConnector,
                                             val auditConnector: AuditConnector,
                                             val appNameConfiguration: Configuration) extends TaxCreditsSummaryService with Auditor {

  override def getTaxCreditExclusion(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Exclusion] = {
    withAudit("getTaxCreditExclusion", Map("nino" -> nino.value)) {
      taxCreditsBrokerConnector.getExclusion(TaxCreditsNino(nino.value))
    }
  }

  override def getTaxCreditSummary(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[TaxCreditSummary] = {
    withAudit("getTaxCreditSummary", Map("nino" -> nino.value)) {

      val tcNino = TaxCreditsNino(nino.value)

      def getChildrenAge16AndUnder: Future[Children] = {
        taxCreditsBrokerConnector.getChildren(tcNino).map(children =>
          Children(Child.getEligibleChildren(children)))
      }

      val childrenFuture = getChildrenAge16AndUnder
      val partnerDetailsFuture = taxCreditsBrokerConnector.getPartnerDetails(tcNino)
      val paymentSummaryFuture = taxCreditsBrokerConnector.getPaymentSummary(tcNino)
      val personalDetailsFuture = taxCreditsBrokerConnector.getPersonalDetails(tcNino)

      for {
        children <- childrenFuture
        partnerDetails <- partnerDetailsFuture
        paymentSummary <- paymentSummaryFuture
        personalDetails <- personalDetailsFuture
      } yield TaxCreditSummary(paymentSummary, personalDetails, partnerDetails, children)
    }
  }
}

object SandboxTaxCreditsSummaryService extends TaxCreditsSummaryService with FileResource {

  override def getTaxCreditSummary(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[TaxCreditSummary] = {
    val resource: String = findResource(s"/resources/taxcreditsummary/${nino.value}.json").getOrElse(throw new IllegalArgumentException("Resource not found!"))
    Future.successful(Json.parse(resource).as[TaxCreditSummary])
  }

  override def getTaxCreditExclusion(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Exclusion] = Future.successful(Exclusion(false))

}
