import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

case class CrossDep(
											 shared: Def.Initialize[Seq[ModuleID]],
											 jvm: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq.empty[ModuleID]),
											 js: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq.empty[ModuleID]))

object Dependencies {

	//libs for testing
  lazy val testing = Def.setting(Seq(
		"org.scalatest" %%% "scalatest" % Versions.scalaTest % Test,

		"org.scalatest" %%% "scalatest-matchers" % Versions.scalaTestMatchers % Test,

		"org.scalatest" %%% "scalatest-wordspec" % Versions.scalaTestMatchers % Test
  ))

	//akka-related libs
	lazy val akka = Def.setting(Seq(

		"org.denigma" %%% "akka-http-extensions" % Versions.akkaHttpExtensions,

		"com.typesafe.akka" %% "akka-http-testkit" % Versions.akka % Test,

		"com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % Test,

		"ch.qos.logback" % "logback-classic" % Versions.logback,

		"com.typesafe.akka" %% "akka-slf4j" % Versions.akka,

		"de.heikoseeberger" %% "akka-http-circe" % Versions.circeHttp
	))

	//scalajs libs
	lazy val sjsLibs= Def.setting(Seq(
		"org.scala-js" %%% "scalajs-dom" % Versions.dom,

		"org.querki" %%% "jquery-facade" % Versions.jqueryFacade, //scalajs facade for jQuery + jQuery extensions

		"org.denigma" %%% "codemirror-facade" % Versions.codemirrorFacade,

		"org.denigma" %%% "threejs-facade" % Versions.threejsFacade,

		"io.circe" %%% "circe-scalajs" % Versions.circe
	))

	//dependencies on javascript libs
	lazy val webjars= Def.setting(Seq(
		"org.webjars" % "Semantic-UI" %  Versions.semanticUI,

		"org.webjars" % "codemirror" % Versions.codemirror,

		"org.webjars" % "jquery" % Versions.jquery,

		"org.webjars" % "jquery-svg" % Versions.jquerySVG,

		"org.webjars" % "three.js" % Versions.threeJS

	))

	//common purpose libs
	lazy val commonShared: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq(
		"com.github.japgolly.scalacss" %%% "core" % Versions.scalaCSS,

		"com.github.japgolly.scalacss" %%% "ext-scalatags" %  Versions.scalaCSS,

		"org.denigma" %%% "binding-controls" % Versions.bindingControls,

		"com.lihaoyi" %%% "fastparse" % Versions.fastparse,

		"io.circe" %%% "circe-core" % Versions.circe,

		"io.circe" %%% "circe-generic" % Versions.circe,

		"io.circe" %%% "circe-parser" % Versions.circe,

		"com.softwaremill.quicklens" %%% "quicklens" % Versions.quicklens
	))

	val otherJvm = Def.setting(Seq(
    "com.lihaoyi" %% "ammonite-ops" % Versions.ammonite,

		"org.sbolstandard" % "libSBOLj-parent" % Versions.libSBOLj,

		"org.scalanlp" %% "breeze" % Versions.breeze,

		"com.iheart" %% "ficus" % Versions.ficus,

		"com.github.pathikrit"  %% "better-files-akka"  % Versions.betterFiles,

		"org.biopax.paxtools" % "paxtools" % Versions.paxtools

		//"org.clulab" %% "reach" % Versions.reach

	))


	val compilers = Def.setting(Seq(
		"org.scala-lang" % "scala-compiler" % Versions.scala
	))
}
