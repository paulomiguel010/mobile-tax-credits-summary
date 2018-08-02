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

import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.auth.core.ConfidenceLevel._
import uk.gov.hmrc.auth.core.syntax.retrieved._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.play.test.WithFakeApplication

import scala.concurrent.{ExecutionContext, Future}

class TaxCreditsSummaryControllerSpec extends TestSetup with WithFakeApplication with FileResource {
  "tax credits summary live" should {
    val controller = new LiveTaxCreditsSummaryController(mockAuthConnector, 200, mockService, mockAuditConnector, mockConfiguration)
    "process the request successfully and filter children older than 20 and where deceased flags are active and user is not excluded" in {
      val expectedResult = TaxCreditsSummaryResponse(excluded = false, Some(TaxCreditsSummary(paymentSummary, Some(claimants))))

      mockAuthorisationGrantAccess(Some(nino) and L200)
      mockAudit(Nino(nino), expectedResult)
      (mockService.getTaxCreditsSummaryResponse(_: Nino)(_: HeaderCarrier, _: ExecutionContext)).
        expects(Nino(nino), *, *).returning(expectedResult)

      val result: Result = await(controller.taxCreditsSummary(Nino(nino))(emptyRequestWithAcceptHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 200
      contentAsJson(result) shouldBe toJson(expectedResult)
    }

    "return 403 when the nino in the request does not match the authority nino" in {
      mockAuthorisationGrantAccess(Some(nino) and L200)

      status(await(controller.taxCreditsSummary(incorrectNino)(emptyRequestWithAcceptHeader(renewalReference, Nino(nino))))) shouldBe 403
    }

    "return 500 given a service error" in {
      mockAuthorisationGrantAccess(Some(nino) and L200)

      (mockService.getTaxCreditsSummaryResponse(_: Nino)(_: HeaderCarrier, _: ExecutionContext)).
        expects(Nino(nino), *, *).returning(Future failed Upstream5xxResponse("error", 500, 500))

      status(await(controller.taxCreditsSummary(Nino(nino))(emptyRequestWithAcceptHeader(renewalReference, Nino(nino))))) shouldBe 500
    }

    "return the summary successfully when journeyId is supplied and user is not excluded" in {
      val expectedResult = TaxCreditsSummaryResponse(excluded = false, Some(TaxCreditsSummary(paymentSummary, Some(claimants))))

      mockAuthorisationGrantAccess(Some(nino) and L200)
      mockAudit(Nino(nino), expectedResult)
      (mockService.getTaxCreditsSummaryResponse(_: Nino)(_: HeaderCarrier, _: ExecutionContext)).
        expects(Nino(nino), *, *).returning(expectedResult)

      val result: Result =
        await(controller.taxCreditsSummary(Nino(nino), Some("journeyId"))(emptyRequestWithAcceptHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 200
      contentAsJson(result) shouldBe toJson(expectedResult)
    }

    "return unauthorized when authority record does not contain a NINO" in {
      mockAuthorisationGrantAccess(None and L200)

      val result: Result = await(controller.taxCreditsSummary(Nino(nino))(emptyRequestWithAcceptHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 401
      contentAsJson(result) shouldBe noNinoFoundOnAccount
    }

    "return unauthorized when authority record has a low CL" in {
      mockAuthorisationGrantAccess(Some(nino) and L100)

      val result: Result = await(controller.taxCreditsSummary(Nino(nino))(emptyRequestWithAcceptHeader(renewalReference, Nino(nino))))
      status(result) shouldBe 401
      contentAsJson(result) shouldBe lowConfidenceLevelError
    }

    "return status code 406 when the headers are invalid" in {
      val result: Result = await(controller.taxCreditsSummary(Nino(nino))(requestInvalidHeaders))
      status(result) shouldBe 406
    }

  }

  "tax credits summary Sandbox" should {
    "return the summary response from a resource" in {
      val controller = new SandboxTaxCreditsSummaryController()
      val result: Result = await(controller.taxCreditsSummary(Nino(nino)).apply(fakeRequest))
      val currentTime = new LocalDate().toDateTimeAtStartOfDay
      val expectedTaxCreditsSummary: TaxCreditsSummary =
        Json.parse(findResource(s"/resources/taxcreditssummary/$nino.json").get
          .replaceAll("previousDate1", currentTime.minusWeeks(2).getMillis.toString)
          .replaceAll("previousDate2", currentTime.minusWeeks(1).getMillis.toString)
          .replaceAll("previousDate3", currentTime.getMillis.toString)
          .replaceAll("date1", currentTime.plusWeeks(1).getMillis.toString)
          .replaceAll("date2", currentTime.plusWeeks(2).getMillis.toString)
          .replaceAll("date3", currentTime.plusWeeks(3).getMillis.toString)
          .replaceAll("date4", currentTime.plusWeeks(4).getMillis.toString)
          .replaceAll("date5", currentTime.plusWeeks(5).getMillis.toString)
          .replaceAll("date6", currentTime.plusWeeks(6).getMillis.toString)
          .replaceAll("date7", currentTime.plusWeeks(7).getMillis.toString)
          .replaceAll("date8", currentTime.plusWeeks(8).getMillis.toString)).as[TaxCreditsSummary]
      val expectedResult: TaxCreditsSummaryResponse = TaxCreditsSummaryResponse(taxCreditsSummary = Some(expectedTaxCreditsSummary))
      contentAsJson(result) shouldBe toJson(expectedResult)
    }
  }

}