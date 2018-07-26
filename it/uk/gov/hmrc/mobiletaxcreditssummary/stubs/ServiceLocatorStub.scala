package uk.gov.hmrc.mobiletaxcreditssummary.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, postRequestedFor, urlPathEqualTo, verify}
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.api.domain.Registration

object ServiceLocatorStub {
  private def regPayloadStringFor(serviceName: String, serviceUrl: String): String =
    toJson(Registration(serviceName, serviceUrl, Some(Map("third-party-api" -> "true")))).toString

  private val registrationPattern: RequestPatternBuilder = postRequestedFor(urlPathEqualTo("/registration"))
    .withHeader("content-type", equalTo("application/json"))
    .withRequestBody(equalTo(regPayloadStringFor("mobile-tax-credits-summary", "https://mobile-tax-credits-summary.protected.mdtp")))

  def registerShouldHaveBeenCalled(): Unit = verify(1, registrationPattern)

  def registerShouldNotHaveBeenCalled(): Unit = verify(0, registrationPattern)
}
