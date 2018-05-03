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

package uk.gov.hmrc.mobiletaxcreditssummary.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.ConfidenceLevel._
import uk.gov.hmrc.auth.core.syntax.retrieved._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobiletaxcreditssummary.domain._
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.mobiletaxcreditssummary.services.TaxCreditsSummaryService
import uk.gov.hmrc.play.test.WithFakeApplication

import scala.concurrent.{ExecutionContext, Future}

class TestTaxCreditsSummaryController(val authConnector: AuthConnector,
                                      val service: TaxCreditsSummaryService,
                                      val confLevel: Int) extends TaxCreditsSummaryController

class TestTaxCreditsSummaryRenewalSummarySpec extends TestSetup with WithFakeApplication with FileResource {

  "tax credits summary live" should {
    "process the request successfully and filter children older than 20 and where deceased flags are active" in new mocks {
      stubAuthorisationGrantAccess(Some(nino) and L200)
      stubTaxCreditBrokerConnectorGetChildren(Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)))
      stubTaxCreditBrokerConnectorGetPartnerDetails(Some(partnerDetails(nino)))
      stubTaxCreditBrokerConnectorGetPersonalDetails(personalDetails(nino))
      stubTaxCreditBrokerConnectorGetPaymentSummary(paymentSummary)
      val expectedResult = TaxCreditSummary(paymentSummary, personalDetails(nino),
        Some(partnerDetails(nino)), Children(Seq(SarahSmith, JosephSmith, MarySmith)))
      override val mockLivePersonalIncomeService = new TestTaxCreditsSummaryService(mockTaxCreditsBrokerConnector, mockAuditConnector, mockConfiguration)
      val controller = new TestTaxCreditsSummaryController(mockAuthConnector, mockLivePersonalIncomeService, 200)
      val result: Result = await(controller.taxCreditsSummary(Nino(nino))(emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 200
      contentAsJson(result) shouldBe toJson(expectedResult)
    }

    "return 401 when the nino in the request does not match the authority nino" in new mocks {
      stubAuthorisationGrantAccess(Some(nino) and L200)
      val controller = new LiveTaxCreditsSummaryController(mockAuthConnector, 200, mockLivePersonalIncomeService)
      status(await(controller.taxCreditsSummary(incorrectNino)(emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))) shouldBe 401
    }

    "return 429 HTTP status when retrieval of children returns 503" in new mocks {
      stubAuthorisationGrantAccess(Some(nino) and L200)
      when(mockTaxCreditsBrokerConnector.getChildren(any[TaxCreditsNino]())(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.failed(new ServiceUnavailableException("controlled explosion kaboom!!")))
      override val mockLivePersonalIncomeService = new TestTaxCreditsSummaryService(mockTaxCreditsBrokerConnector, mockAuditConnector, mockConfiguration)
      val controller = new TestTaxCreditsSummaryController(mockAuthConnector, mockLivePersonalIncomeService, 200)
      status(await(controller.taxCreditsSummary(Nino(nino))(emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))) shouldBe 429
    }

    "return the summary successfully when journeyId is supplied" in new mocks {
      stubAuthorisationGrantAccess(Some(nino) and L200)
      stubTaxCreditBrokerConnectorGetChildren(Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)))
      stubTaxCreditBrokerConnectorGetPartnerDetails(Some(partnerDetails(nino)))
      stubTaxCreditBrokerConnectorGetPersonalDetails(personalDetails(nino))
      stubTaxCreditBrokerConnectorGetPaymentSummary(paymentSummary)
      val expectedResult = TaxCreditSummary(paymentSummary, personalDetails(nino),
        Some(partnerDetails(nino)), Children(Seq(SarahSmith, JosephSmith, MarySmith)))
      override val mockLivePersonalIncomeService =
        new TestTaxCreditsSummaryService(mockTaxCreditsBrokerConnector, mockAuditConnector, mockConfiguration)
      val controller = new TestTaxCreditsSummaryController(mockAuthConnector, mockLivePersonalIncomeService, 200)
      val result: Result = await(controller.taxCreditsSummary(Nino(nino), Some("unique-journey-id"))
      (emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 200
      contentAsJson(result) shouldBe toJson(expectedResult)
    }

    "return unauthorized when authority record does not contain a NINO" in new mocks {
      stubAuthorisationGrantAccess(None and L200)
      val controller = new LiveTaxCreditsSummaryController(mockAuthConnector, 200, mockLivePersonalIncomeService)
      val result: Result = await(controller.taxCreditsSummary(Nino(nino))(emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 401
      contentAsJson(result) shouldBe noNinoFoundOnAccount
    }

    "return unauthorized when authority record has a low CL" in new mocks {
      stubAuthorisationGrantAccess(Some(nino) and L100)
      val controller = new LiveTaxCreditsSummaryController(mockAuthConnector, 200, mockLivePersonalIncomeService)
      val result: Result = await(controller.taxCreditsSummary(Nino(nino))(emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 401
      contentAsJson(result) shouldBe lowConfidenceLevelError
    }

    "return status code 406 when the headers are invalid" in new mocks {
      stubAuthorisationGrantAccess(Some(nino) and L200)
      val controller = new LiveTaxCreditsSummaryController(mockAuthConnector, 200, mockLivePersonalIncomeService)
      val result: Result = await(controller.taxCreditsSummary(Nino(nino))(requestInvalidHeaders))
      status(result) shouldBe 406
    }
  }

  "tax credits summary Sandbox" should {
    "return the summary response from a resource" in new mocks {
      val controller = new SandboxTaxCreditsSummaryController(mockAuthConnector, 200)
      val result: Result = await(controller.taxCreditsSummary(Nino(nino)).apply(fakeRequest))
      contentAsJson(result) shouldBe Json.parse(findResource(s"/resources/taxcreditsummary/$nino.json").get)
    }
  }

}

class TestExclusionsServiceSpec extends TestSetup with WithFakeApplication {

  "tax exclusions service" should {
    "process the request for get tax credit exclusion successfully" in new mocks {
      stubAuthorisationGrantAccess(Some(nino) and L200)
      stubTaxCreditBrokerConnectorGetExclusion(Exclusion(false))
      override implicit val mockLivePersonalIncomeService: TestTaxCreditsSummaryService =
        new TestTaxCreditsSummaryService(mockTaxCreditsBrokerConnector, mockAuditConnector, mockConfiguration)
      val controller = new TestTaxCreditsSummaryController(mockAuthConnector, mockLivePersonalIncomeService, 200)
      val result: Result = await(controller.getTaxCreditExclusion(Nino(nino)).apply(emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.parse("""{"showData":true}""")

    }

    "return 401 when the nino in the request does not match the authority nino" in new mocks {
      stubAuthorisationGrantAccess(None and L200)
      val controller = new LiveTaxCreditsSummaryController(mockAuthConnector, 200, mockLivePersonalIncomeService)
      val result: Result = await(controller.getTaxCreditExclusion(Nino(nino))(emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 401
      contentAsJson(result) shouldBe noNinoFoundOnAccount
    }

    "return 429 HTTP status when get tax credit exclusion returns 503" in new mocks {
      stubAuthorisationGrantAccess(Some(nino) and L200)
      when(mockTaxCreditsBrokerConnector.getExclusion(any[TaxCreditsNino]())(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.failed(new ServiceUnavailableException("controlled explosion kaboom!!")))
      override val mockLivePersonalIncomeService = new TestTaxCreditsSummaryService(mockTaxCreditsBrokerConnector, mockAuditConnector, mockConfiguration)
      val controller = new TestTaxCreditsSummaryController(mockAuthConnector, mockLivePersonalIncomeService, 200)
      status(await(controller.getTaxCreditExclusion(Nino(nino))(emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))) shouldBe 429
    }

    "return the tax credit exclusion successfully when journeyId is supplied" in new mocks {
      stubAuthorisationGrantAccess(Some(nino) and L200)
      stubTaxCreditBrokerConnectorGetExclusion(Exclusion(false))
      override implicit val mockLivePersonalIncomeService: TestTaxCreditsSummaryService =
        new TestTaxCreditsSummaryService(mockTaxCreditsBrokerConnector, mockAuditConnector, mockConfiguration)
      val controller = new TestTaxCreditsSummaryController(mockAuthConnector, mockLivePersonalIncomeService, 200)
      val result: Result = await(controller.getTaxCreditExclusion(Nino(nino), Some("unique-journey-id"))
        .apply(emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.parse("""{"showData":true}""")
    }

    "return unauthorized when authority record does not contain a NINO" in new mocks {
      stubAuthorisationGrantAccess(None and L200)
      val controller = new LiveTaxCreditsSummaryController(mockAuthConnector, 200, mockLivePersonalIncomeService)
      val result: Result = await(controller.getTaxCreditExclusion(Nino(nino))(emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 401
      contentAsJson(result) shouldBe noNinoFoundOnAccount
    }

    "return unauthorized when authority record has a low Confidence Level" in new mocks {
      stubAuthorisationGrantAccess(Some(nino) and L100)
      val controller = new LiveTaxCreditsSummaryController(mockAuthConnector, 200, mockLivePersonalIncomeService)
      val result: Result = await(controller.getTaxCreditExclusion(Nino(nino))(emptyRequestWithAcceptHeaderAndAuthHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 401
      contentAsJson(result) shouldBe lowConfidenceLevelError
    }

    "return status code 406 when the headers are invalid" in new mocks {
      stubAuthorisationGrantAccess(Some(nino) and L200)
      val controller = new LiveTaxCreditsSummaryController(mockAuthConnector, 200, mockLivePersonalIncomeService)
      val result: Result = await(controller.getTaxCreditExclusion(Nino(nino))(requestInvalidHeaders))
      status(result) shouldBe 406
    }
  }
}