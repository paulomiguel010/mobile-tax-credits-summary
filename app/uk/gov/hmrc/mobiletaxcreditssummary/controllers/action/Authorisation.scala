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

package uk.gov.hmrc.mobiletaxcreditssummary.controllers.action

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.controllers.{AccountWithLowCL, FailToMatchTaxIdOnAuth, NinoNotFoundOnAccount, _}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Authority(nino: Nino)

trait Authorisation extends Results with AuthorisedFunctions {

  val confLevel: Int

  lazy val requiresAuth: Boolean = true
  lazy val ninoNotFoundOnAccount = new NinoNotFoundOnAccount
  lazy val failedToMatchNino = new FailToMatchTaxIdOnAuth
  lazy val lowConfidenceLevel = new AccountWithLowCL

  def grantAccess(requestedNino: Nino)(implicit hc: HeaderCarrier): Future[Authority] = {
    authorised(Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", requestedNino.value)), "Activated", None))
      .retrieve(nino and confidenceLevel) {
        case Some(foundNino) ~ foundConfidenceLevel =>
          if (foundNino.isEmpty) throw ninoNotFoundOnAccount
          if (!foundNino.equals(requestedNino.nino)) throw failedToMatchNino
          if (confLevel > foundConfidenceLevel.level) throw lowConfidenceLevel
          Future(Authority(requestedNino))
        case None ~ _ =>
          throw ninoNotFoundOnAccount
      }
  }

  def invokeAuthBlock[A](request: Request[A], block: Request[A] => Future[Result], taxId: Option[Nino]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)

    grantAccess(taxId.getOrElse(Nino(""))).flatMap { _ =>
      block(request)
    }.recover {
      case _: uk.gov.hmrc.http.Upstream4xxResponse =>
        Logger.info("Unauthorized! Failed to grant access since 4xx response!")
        Unauthorized(Json.toJson(ErrorUnauthorizedMicroService))

      case _: NinoNotFoundOnAccount =>
        Logger.info("Unauthorized! NINO not found on account!")
        Unauthorized(Json.toJson(ErrorUnauthorizedNoNino))

      case _: FailToMatchTaxIdOnAuth =>
        Logger.info("Forbidden! Failure to match URL NINO against Auth NINO")
        Forbidden(Json.toJson(ErrorForbidden))

      case _: AccountWithLowCL =>
        Logger.info("Unauthorized! Account with low CL!")
        Unauthorized(Json.toJson(ErrorUnauthorizedLowCL))
    }
  }
}

trait AccessControl extends HeaderValidator with Authorisation {

  def validateAcceptWithAuth(rules: Option[String] => Boolean, taxId: Option[Nino]): ActionBuilder[Request] = new ActionBuilder[Request] {

    def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
      if (rules(request.headers.get("Accept"))) {
        if (requiresAuth) invokeAuthBlock(request, block, taxId)
        else block(request)
      }
      else Future.successful(Status(ErrorAcceptHeaderInvalid.httpStatusCode)(Json.toJson(ErrorAcceptHeaderInvalid)))
    }
  }
}