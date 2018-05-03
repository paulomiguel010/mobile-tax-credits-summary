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

package uk.gov.hmrc.mobiletaxcreditssummary.domain

import com.ning.http.util.Base64
import play.api.libs.json.{Json, OFormat}

import scala.util.Try

case class RenewalReference(value: String) {
  def stripSpaces = RenewalReference(value.replaceAll(" ", ""))
}

case class TcrAuthenticationToken(tcrAuthToken: String) {
  def extractNino: Option[String] = extractBasicAuth(tcrAuthToken).map(_._1)

  def extractRenewalReference: Option[String] = extractBasicAuth(tcrAuthToken).map(_._2)

  private def extractBasicAuth(auth: String): Option[(String, String)] = {
    val BasicAuthPattern = "Basic (.*)".r

    Try {
      auth match {
        case BasicAuthPattern(encoded) =>
          val parts = new String(Base64.decode(encoded)).split(":")
          (parts(0), parts(1))
      }
    }.toOption
  }

}

object TcrAuthenticationToken {
  implicit val formats: OFormat[TcrAuthenticationToken] = Json.format[TcrAuthenticationToken]

  def basicAuthString(nino: String, renewalReference: String): String = "Basic " + encodedAuth(nino, renewalReference)

  def encodedAuth(nino: String, renewalReference: String): String = new String(Base64.encode(s"$nino:$renewalReference".getBytes))

}