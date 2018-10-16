import play.core.PlayVersion
import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning

val appName: String = "mobile-tax-credits-summary"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory): _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(publishingSettings: _*)
  .settings(routesImport ++= Seq("uk.gov.hmrc.domain._", "uk.gov.hmrc.mobiletaxcreditssummary.binders.Binders._"))
  .settings(routesGenerator := StaticRoutesGenerator)
  .settings(
    majorVersion := 0,
    playDefaultPort := 8246,
    libraryDependencies ++= compile ++ test ++ integration,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers += Resolver.jcenterRepo,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest) (base => Seq(base / "it")).value,
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value)
  )

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests map {
    test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }

val compile = Seq(
  "uk.gov.hmrc" %% "bootstrap-play-25" % "3.10.0",
  "uk.gov.hmrc" %% "auth-client" % "2.10.0",
  "uk.gov.hmrc" %% "play-hmrc-api" % "3.2.0",
  "uk.gov.hmrc" %% "domain" % "5.2.0",
  "uk.gov.hmrc" %% "play-ui" % "7.22.0"
)

val test = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.1.0" % Test,
  "org.scalamock" %% "scalamock" % "4.0.0" % Test
)

val integration = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.1.0" % IntegrationTest,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % IntegrationTest,
  "info.cukes" %% "cucumber-scala" % "1.2.5" % IntegrationTest,
  "info.cukes" % "cucumber-junit" % "1.2.5" % IntegrationTest,
  "com.github.tomakehurst" % "wiremock" % "2.10.1" % IntegrationTest,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % IntegrationTest
)
