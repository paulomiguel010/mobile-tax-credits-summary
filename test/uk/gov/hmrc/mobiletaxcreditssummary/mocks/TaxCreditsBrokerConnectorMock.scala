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

package uk.gov.hmrc.mobiletaxcreditssummary.mocks

import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors.TaxCreditsBrokerConnector
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}

trait TaxCreditsBrokerConnectorMock extends MockFactory {

  val expectedNextDueDate: DateTime = DateTime.parse("2015-07-16")
  val expectedPaymentWTC = FuturePayment(160.34, expectedNextDueDate, oneOffPayment = false)
  val expectedPaymentCTC = FuturePayment(140.12, expectedNextDueDate, oneOffPayment = false)
  val paymentSectionCTC = PaymentSection(List(expectedPaymentCTC), "weekly")
  val paymentSectionWTC = PaymentSection(List(expectedPaymentWTC), "weekly")
  val paymentSummary = PaymentSummary(Some(paymentSectionWTC), Some(paymentSectionCTC), paymentEnabled = Some(true))

  val AGE16: DateTime = DateTimeUtils.now.minusYears(16)
  val AGE15: DateTime = DateTimeUtils.now.minusYears(15)
  val AGE13: DateTime = DateTimeUtils.now.minusYears(13)
  val AGE21: DateTime = DateTimeUtils.now.minusYears(21)
  val DECEASED_DATE: DateTime = DateTimeUtils.now.minusYears(1)

  val SarahSmith = Child("Sarah", "Smith", new DateTime(AGE16), hasFTNAE = false, hasConnexions = false, isActive = true, None)
  val JosephSmith = Child("Joseph", "Smith", new DateTime(AGE15), hasFTNAE = false, hasConnexions = false, isActive = true, None)
  val MarySmith = Child("Mary", "Smith", new DateTime(AGE13), hasFTNAE = false, hasConnexions = false, isActive = true, None)
  val JennySmith = Child("Jenny", "Smith", new DateTime(AGE21), hasFTNAE = false, hasConnexions = false, isActive = true, None)
  val PeterSmith = Child("Peter", "Smith", new DateTime(AGE13), hasFTNAE = false, hasConnexions = false, isActive = false, Some(new DateTime(DECEASED_DATE)))
  val SimonSmith = Child("Simon", "Smith", new DateTime(AGE13), hasFTNAE = false, hasConnexions = false, isActive = true, Some(new DateTime(DECEASED_DATE)))

  val personalDetails = Person(forename = "firstname", surname = "surname")

  val partnerDetails = Person(forename = "forename", surname = "surname")

  val claimants = Claimants(personalDetails, Some(partnerDetails), Seq(
    Person(forename = "Sarah", surname = "Smith"), Person(forename = "Joseph", surname = "Smith"), Person(forename = "Mary", surname = "Smith")))

  val claimantsNoPartnerDetails = Claimants(personalDetails, None, Seq(
    Person(forename = "Sarah", surname = "Smith"), Person(forename = "Joseph", surname = "Smith"), Person(forename = "Mary", surname = "Smith")))

  val claimantsNoChildren = Claimants(personalDetails, Some(partnerDetails), Seq.empty)

  def mockTaxCreditsBrokerConnectorGetExclusion(response: Option[Exclusion], nino: TaxCreditsNino)(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector.getExclusion(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext)).expects(nino, *, *).returning(Future successful response)

  def mockTaxCreditsBrokerConnectorGetExclusionFailure(response: Exception, nino: TaxCreditsNino)(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector.getExclusion(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext)).expects(nino, *, *).returning(Future failed response)

  def mockTaxCreditsBrokerConnectorGetPaymentSummary(response: PaymentSummary, nino: TaxCreditsNino)(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector.getPaymentSummary(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext)).expects(nino, *, *).returning(Future successful response)

  def mockTaxCreditsBrokerConnectorGetPaymentFailure(response: Exception, nino: TaxCreditsNino)(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector.getPaymentSummary(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext)).expects(nino, *, *).returning(Future failed response)

  def mockTaxCreditsBrokerConnectorGetChildren(response: Seq[Child], nino: TaxCreditsNino)(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector.getChildren(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext)).expects(nino, *, *).returning(Future successful response)

  def mockTaxCreditsBrokerConnectorGetChildrenFailure(response: Exception, nino: TaxCreditsNino)(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector.getChildren(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext)).expects(nino, *, *).returning(Future failed response)

  def mockTaxCreditsBrokerConnectorGetPartnerDetails(response: Option[Person], nino: TaxCreditsNino)(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector.getPartnerDetails(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext)).expects(nino, *, *).returning(Future successful response)

  def mockTaxCreditsBrokerConnectorGetPartnerDetailsFailure(response: Exception, nino: TaxCreditsNino)(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector.getPartnerDetails(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext)).expects(nino, *, *).returning(Future failed response)

  def mockTaxCreditsBrokerConnectorGetPersonalDetails(response: Person, nino: TaxCreditsNino)(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector.getPersonalDetails(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext)).expects(nino, *, *).returning(Future successful response)

  def mockTaxCreditsBrokerConnectorGetPersonalDetailsFailure(response: Exception, nino: TaxCreditsNino)(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector.getPersonalDetails(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext)).expects(nino, *, *).returning(Future failed response)
}
