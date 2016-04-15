import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.gzip.Import.gzip
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.{PathMapping, SbtWeb}
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPluginInternal
import playscalajs.PlayScalaJS
import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin.autoImport._

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
  // Enable JAR export for staging
  exportJars := true,
  resolvers += sbt.Resolver.bintrayRepo("denigma", "denigma-releases"), //for scala-js-binding
  resolvers += Resolver.jcenterRepo,
  unmanagedClasspath in Compile <++= unmanagedResources in Compile,
  libraryDependencies ++= Dependencies.commonShared.value ++ Dependencies.testing.value,
  updateOptions := updateOptions.value.withCachedResolution(true) //to speed up dependency resolution
) ++ eclipseSettings


val scalaJSDevStage  = Def.taskKey[Pipeline.Stage]("Apply fastOptJS on all Scala.js projects")

def scalaJSDevTaskStage: Def.Initialize[Task[Pipeline.Stage]] = Def.task { mappings: Seq[PathMapping] =>
  mappings ++ PlayScalaJS.devFiles(Compile).value ++ PlayScalaJS.sourcemapScalaFiles(fastOptJS).value
}

lazy val app = crossProject
  .crossType(CrossType.Full)
  .in(file("app"))
  .settings(commonSettings ++ publishSettings: _*)
  .settings(
    name := "kappa-notebook",
    version := Versions.kappaNotebook
  ).disablePlugins(RevolverPlugin).
    // adding the `it` configuration
    configs(IntegrationTest).
    // adding `it` tasks
    settings(Defaults.itSettings:_*).
    // add `shared` folder to `jvm` source directories
    jvmSettings(unmanagedSourceDirectories in IntegrationTest ++=
    CrossType.Full.sharedSrcDir(baseDirectory.value, "it").toSeq).
    // add `shared` folder to `js` source directories
    jsSettings(unmanagedSourceDirectories in IntegrationTest ++=
    CrossType.Full.sharedSrcDir(baseDirectory.value, "it").toSeq).
  // adding ScalaJSClassLoader to `js` configuration
  jsSettings(inConfig(IntegrationTest)(ScalaJSPluginInternal.scalaJSTestSettings):_*)
  .jsSettings(
    libraryDependencies ++= Dependencies.sjsLibs.value,
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    jsDependencies += RuntimeDOM % Test
    //jsEnv in Test := new org.scalajs.jsenv.selenium.SeleniumJSEnv(org.scalajs.jsenv.selenium.Firefox)
  )
  .jsConfigure(p=>p.enablePlugins(ScalaJSPlay))
  .jvmSettings(
    libraryDependencies ++= Dependencies.akka.value ++ Dependencies.webjars.value,
    mainClass in Compile := Some("org.denigma.kappa.notebook.Main"),
    libraryDependencies ++= Dependencies.compilers.value ++ Dependencies.otherJvm.value,
    scalaJSDevStage := scalaJSDevTaskStage.value,
    //pipelineStages := Seq(scalaJSProd,gzip),
    (emitSourceMaps in fullOptJS) := true,
    parallelExecution in Test := false,
    pipelineStages in Assets := Seq(scalaJSDevStage, gzip), //for run configuration
    (fullClasspath in Runtime) += (packageBin in Assets).value, //to package production deps
    libraryDependencies += "com.lihaoyi" %% "ammonite-ops" % Versions.ammonite,
    libraryDependencies += "com.lihaoyi" %% "ammonite-shell" % Versions.ammonite,
    libraryDependencies += "com.lihaoyi" % "ammonite-repl" % Versions.ammonite % Test cross CrossVersion.full,
    initialCommands in (Test, console) := Console.out
  )
  .jvmConfigure(p => p.enablePlugins(SbtTwirl, SbtWeb, PlayScalaJS))

lazy val appJS = app.js
lazy val appJVM = app.jvm settings (scalaJSProjects := Seq(appJS))

lazy val root = Project("root",file("."),settings = commonSettings)
  .settings(
    name := "kappa-notebook",
    version := Versions.kappaNotebook,
    mainClass in Compile := (mainClass in appJVM in Compile).value,
    (fullClasspath in Runtime) += (packageBin in appJVM in Assets).value,
    maintainer := "Anton Kulaga <antonkulaga@gmail.com>",
    packageSummary := "kappa-notebook",
    packageDescription := """Kappa notebook runs kappa from the browser""",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint", "-J-Xss5M"),
    initialCommands in (Test, console) := Console.out
  ) dependsOn appJVM aggregate(appJVM, appJS) enablePlugins JavaServerAppPackaging
