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

import javax.inject.{Inject, Named, Singleton}
import play.api._
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc._
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.api.service.Auditor
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, ServiceUnavailableException}
import uk.gov.hmrc.mobiletaxcreditssummary.controllers.action.AccessControl
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.mobiletaxcreditssummary.services.LiveTaxCreditsSummaryService
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ErrorHandling {
  self: BaseController =>

  def notFound: Result = Status(ErrorNotFound.httpStatusCode)(toJson(ErrorNotFound))

  def errorWrapper(func: => Future[mvc.Result])(implicit hc: HeaderCarrier): Future[Result] = {
    func.recover {
      case _: NotFoundException => notFound

      case ex: ServiceUnavailableException =>
        // The hod can return a 503 HTTP status which is translated to a 429 response code.
        // The 503 HTTP status code must only be returned from the API gateway and not from downstream API's.
        Logger.error(s"ServiceUnavailableException reported: ${ex.getMessage}", ex)
        Status(ClientRetryRequest.httpStatusCode)(toJson(ClientRetryRequest))

      case e: Throwable =>
        Logger.error(s"Internal server error: ${e.getMessage}", e)
        Status(ErrorInternalServerError.httpStatusCode)(toJson(ErrorInternalServerError))
    }
  }
}

trait TaxCreditsSummaryController extends BaseController {

  def taxCreditsSummary(nino: Nino, journeyId: Option[String] = None): Action[AnyContent]

  def shutteredTaxCreditsSummaryResponse(shutteredMessage: String): Result =
    Ok(toJson(
      TaxCreditsSummaryResponse(
        excluded = false,
        Some(TaxCreditsSummary(ShutteredPaymentSummary(Some(shutteredMessage.replaceAll("_", " "))), None)))))
}

@Singleton
class LiveTaxCreditsSummaryController @Inject()(override val authConnector: AuthConnector,
                                                @Named("controllers.confidenceLevel") override val confLevel: Int,
                                                val service: LiveTaxCreditsSummaryService,
                                                val auditConnector: AuditConnector,
                                                val appNameConfiguration: Configuration,
                                                @Named("tax-credits-broker.shuttered") val shuttered: Boolean = false,
                                                @Named("tax-credits-broker.shutteredMessage") val shutteredMessage: String = "")
  extends TaxCreditsSummaryController with AccessControl with ErrorHandling with Auditor {

  override final def taxCreditsSummary(nino: Nino, journeyId: Option[String] = None): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules, Option(nino)).async {
      implicit request =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)

        errorWrapper {
          if (shuttered) {
            Future successful shutteredTaxCreditsSummaryResponse(shutteredMessage)
          } else {
            service.getTaxCreditsSummaryResponse(nino).map { summary =>
              sendAuditEvent(nino, summary, request.path)
              Ok(toJson(summary))
            }
          }
        }
    }

  private def sendAuditEvent(nino: Nino, response: TaxCreditsSummaryResponse, path: String)(implicit hc: HeaderCarrier): Unit = {
    auditConnector.sendExtendedEvent(
      ExtendedDataEvent(
        appName,
        "TaxCreditsSummaryResponse",
        tags = hc.toAuditTags("view-tax-credit-summary", path),
        detail = obj("nino" -> nino.value, "summaryData" -> response)))
  }
}