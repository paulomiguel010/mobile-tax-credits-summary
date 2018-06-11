/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.mobiletaxcreditssummary

import play.api.libs.ws.WSRequest
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.AuthStub.grantAccess
import uk.gov.hmrc.mobiletaxcreditssummary.support.BaseISpec

trait ShutteringSetup extends BaseISpec with FileResource {
  def shutteringMessage2: (String, String)

  val title = "Service Unavailable"
  val message1 = "You'll be able to use the app to manage your tax credits at 9am on Monday 29 May 2017."
  val message2 = "Go to GOV UK to <a href=“https://www.gov.uk“>manage your tax credits online</a>."

  val base64EncodedTitle = "U2VydmljZSBVbmF2YWlsYWJsZQ=="
  val base64EncodedMessage1 = "WW91J2xsIGJlIGFibGUgdG8gdXNlIHRoZSBhcHAgdG8gbWFuYWdlIHlvdXIgdGF4IGNyZWRpdHMgYXQgOWFtIG9uIE1vbmRheSAyOSBNYXkgMjAxNy4="
  val base64EncodedMessage2 = "R28gdG8gR09WIFVLIHRvIDxhIGhyZWY94oCcaHR0cHM6Ly93d3cuZ292LnVr4oCcPm1hbmFnZSB5b3VyIHRheCBjcmVkaXRzIG9ubGluZTwvYT4u"

  override def config: Map[String, Any] = super.config ++ Map(
    "shuttering.shuttered" -> true,
    "shuttering.title" -> base64EncodedTitle,
    "shuttering.message1" -> base64EncodedMessage1,
    shutteringMessage2
  )

  def request(nino: Nino): WSRequest = wsUrl(s"/income/${nino.value}/tax-credits/tax-credits-summary").withHeaders(acceptJsonHeader)
}

class ShutteringISpec extends ShutteringSetup {

  override def shutteringMessage2: (String, String) = "shuttering.message2" -> base64EncodedMessage2

  "GET /income/:nino/tax-credits/tax-credits-summary " should {

    "return a shuttered payload with both messages" in {
      grantAccess(nino1.value)

      val response = await(request(nino1).get())
      response.status shouldBe 503
      (response.json \ "title").as[String] shouldBe title
      (response.json \ "shuttered").as[Boolean] shouldBe true
      (response.json \ "messages").as[Seq[String]] shouldBe Seq(message1, message2)
    }
  }
}

class ShutteringEmptySecondMessageISpec extends ShutteringSetup {

  override def shutteringMessage2: (String, String) = "shuttering.message2" -> ""

  "GET /income/:nino/tax-credits/tax-credits-summary " should {

    "return a shuttered payload with only 1 message" in {
      grantAccess(nino1.value)

      val response = await(request(nino1).get())
      response.status shouldBe 503
      (response.json \ "title").as[String] shouldBe title
      (response.json \ "shuttered").as[Boolean] shouldBe true
      (response.json \ "messages").as[Seq[String]] shouldBe Seq(message1)
    }
  }
}
