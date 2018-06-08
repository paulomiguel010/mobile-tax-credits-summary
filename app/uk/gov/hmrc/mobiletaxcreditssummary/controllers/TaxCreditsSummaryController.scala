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
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, ServiceUnavailableException}
import uk.gov.hmrc.mobiletaxcreditssummary.controllers.action.AccessControl
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.mobiletaxcreditssummary.services.LiveTaxCreditsSummaryService
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession
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

}

@Singleton
class SandboxTaxCreditsSummaryController() extends TaxCreditsSummaryController with FileResource {
  override final def taxCreditsSummary(nino: Nino, journeyId: Option[String] = None): Action[AnyContent] = Action.async {
    implicit request =>
      Future successful (request.headers.get("SANDBOX-CONTROL") match {
        case Some("NON-TAX-CREDITS-USER") => Ok(toJson(TaxCreditsSummaryResponse(taxCreditsSummary = None)))
        case Some("EXCLUDED-TAX-CREDITS-USER") => Ok(toJson(TaxCreditsSummaryResponse(excluded = true, taxCreditsSummary = None)))
        case Some("ERROR-401") => Unauthorized
        case Some("ERROR-403") => Forbidden
        case Some("ERROR-500") => InternalServerError
        case _ => //TAX-CREDITS-USER
          val resource: String = findResource(s"/resources/taxcreditssummary/${nino.value}.json").getOrElse(throw new IllegalArgumentException("Resource not found!"))
          val response = TaxCreditsSummaryResponse(excluded = false, Some(Json.parse(resource).as[TaxCreditsSummary]))
          Ok(toJson(response))
      })
  }
}

@Singleton
class LiveTaxCreditsSummaryController @Inject()(override val authConnector: AuthConnector,
                                                @Named("controllers.confidenceLevel") override val confLevel: Int,
                                                val service: LiveTaxCreditsSummaryService) extends TaxCreditsSummaryController with AccessControl with ErrorHandling {

  override final def taxCreditsSummary(nino: Nino, journeyId: Option[String] = None): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules, Option(nino)).async {
      implicit request =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
        errorWrapper {
          service.getTaxCreditsSummaryResponse(nino).map{ summary =>
            Ok(toJson(summary))
          }
        }
    }
}
