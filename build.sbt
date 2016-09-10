import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.gzip.Import.gzip
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

lazy val bintrayPublishIvyStyle = settingKey[Boolean]("=== !publishMavenStyle") //workaround for sbt-bintray bug

lazy val publishSettings = Seq(
  bintrayRepository := "denigma-releases",
  bintrayOrganization := Some("denigma"),
  licenses += ("MPL-2.0", url("http://opensource.org/licenses/MPL-2.0")),
  bintrayPublishIvyStyle := true
)

/**
 * For parts of the project that we will not publish
 */
lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

//settings for all the projects
lazy val commonSettings = Seq(
  scalaVersion := Versions.scala,
  organization := "org.denigma",
  scalacOptions ++= Seq( "-feature", "-language:_" ),
  javacOptions ++= Seq("-encoding", "UTF-8"),
  parallelExecution in Test := false,
  resolvers += sbt.Resolver.bintrayRepo("denigma", "denigma-releases"), //for scala-js-binding
  resolvers += Resolver.jcenterRepo,
  unmanagedClasspath in Compile <++= unmanagedResources in Compile,
  libraryDependencies ++= Dependencies.commonShared.value ++ Dependencies.testing.value,
  updateOptions := updateOptions.value.withCachedResolution(true) //to speed up dependency resolution
)

lazy val websim = crossProject
  .crossType(CrossType.Full)
  .in(file("websim")) //websim api
  .settings(commonSettings ++ publishSettings: _*)
  .settings(
    name := "websim",
    version := Versions.websim
  ).disablePlugins(RevolverPlugin)
  .jsConfigure(p=>p.enablePlugins(ScalaJSWeb))
  .jsSettings(
    persistLauncher in Compile := true,
    persistLauncher in Test := false
  )
  .jvmSettings(
    libraryDependencies ++= Dependencies.akka.value ++ Dependencies.otherJvm.value,
    (emitSourceMaps in fullOptJS) := true
  )

lazy val websimJS = websim.js
lazy val websimJVM = websim.jvm

lazy val app = crossProject
  .crossType(CrossType.Full)
  .in(file("app"))
  .settings(commonSettings ++ publishSettings: _*)
  .settings(
    name := "kappa-notebook",
    version := Versions.kappaNotebook,
    libraryDependencies ++= Dependencies.appShared.value
  ).dependsOn(websim % "test->test;compile->compile" )
  .disablePlugins(RevolverPlugin)
  .jsSettings(
    libraryDependencies ++= Dependencies.sjsLibs.value,
    persistLauncher in Compile := true,
    emitSourceMaps in fullOptJS := true,
    persistLauncher in Test := false,
    jsDependencies += RuntimeDOM % Test
  )
  .jsConfigure(p=>p.enablePlugins(ScalaJSWeb))
  .jvmSettings(
    mainClass in Compile := Some("org.denigma.kappa.notebook.Main"),
    pipelineStages in Assets := Seq(scalaJSPipeline, gzip),
    compile in Compile <<= (compile in Compile) dependsOn scalaJSPipeline.map(f => f(Seq.empty)),
    libraryDependencies ++= Dependencies.compilers.value ++ Dependencies.otherJvm.value,
    (emitSourceMaps in fullOptJS) := true,
    isDevMode in scalaJSPipeline := { sys.env.get("APP_MODE") match {
        case Some(str) if str.toLowerCase.startsWith("prod") =>
	        println("PRODUCTION MODE")
          false
        case Some(str)  if str.toLowerCase.startsWith("dev") =>
          println("DEVELOPMENT MODE")
          true
        case other =>
          true
          //(devCommands in scalaJSPipeline).value.contains(state.value.history.current)
      }
    },
    libraryDependencies ++= Dependencies.akka.value ++ Dependencies.webjars.value
  )
  .jvmConfigure(p => p.enablePlugins(SbtTwirl, SbtWeb))

lazy val appJS = app.js
lazy val appJVM = app.jvm settings (scalaJSProjects := Seq(appJS))


lazy val root = Project("root",file("."),settings = commonSettings)
  .settings(
    name := "kappa-notebook-root",
    version := Versions.kappaNotebook,
    mainClass in Compile := (mainClass in appJVM in Compile).value,
    (managedClasspath in Runtime) += (packageBin in appJVM in Assets).value,
    maintainer := "Anton Kulaga <antonkulaga@gmail.com>",
    packageSummary := "kappa-notebook",
    packageDescription := """Kappa notebook runs kappa from the browser""",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint", "-J-Xss5M"),
    scalacOptions += "-target:jvm-1.8"
  ) dependsOn appJVM aggregate(appJVM, appJS) enablePlugins JavaAppPackaging
