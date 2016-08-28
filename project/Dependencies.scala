import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

case class CrossDep(
											 shared: Def.Initialize[Seq[ModuleID]],
											 jvm: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq.empty[ModuleID]),
											 js: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq.empty[ModuleID]))

object Dependencies {

	//libs for testing
  lazy val testing = Def.setting(Seq(
		"org.scalatest" %%% "scalatest" % Versions.scalaTest % Test
  ))

	//akka-related libs
	lazy val akka = Def.setting(Seq(

		"org.denigma" %%% "akka-http-extensions" % Versions.akkaHttpExtensions,

		"com.github.pathikrit"  %% "better-files-akka"  % Versions.betterFiles,

		"ch.qos.logback" % "logback-classic" % Versions.logback,

		"com.typesafe.akka" %% "akka-slf4j" % Versions.akka,

		"de.heikoseeberger" %% "akka-http-circe" % Versions.circeHttp,

		"com.typesafe.akka" %% "akka-http-testkit" % Versions.akka % Test,

		"com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % Test

	))

	//scalajs libs
	lazy val sjsLibs= Def.setting(Seq(
		"org.scala-js" %%% "scalajs-dom" % Versions.dom,

		"org.denigma" %%% "codemirror-facade" % Versions.codemirrorFacade,

		"org.denigma" %%% "threejs-facade" % Versions.threejsFacade,

		"io.circe" %%% "circe-scalajs" % Versions.circe,

		"org.denigma" %%% "pdf-js-facade" % Versions.pdfJSFacade,

		"org.singlespaced" %%% "scalajs-d3" % Versions.d3jsFacade
	))

	//dependencies on javascript libs
	lazy val webjars= Def.setting(Seq(
		"org.webjars" % "Semantic-UI" %  Versions.semanticUI,

		"org.webjars" % "codemirror" % Versions.codemirror,

		"org.webjars" % "jquery" % Versions.jquery,

		"org.webjars" % "jquery-svg" % Versions.jquerySVG,

		"org.webjars" % "three.js" % Versions.threeJS,

		"org.webjars" % "d3js" % Versions.d3js,

		"org.webjars.bower" % "malihu-custom-scrollbar-plugin" % Versions.malihuScrollBar,

		"org.webjars" % "jquery.scrollTo" % Versions.scrollTo

	))

	lazy val appShared: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq(
		"com.github.japgolly.scalacss" %%% "core" % Versions.scalaCSS,

		"com.github.japgolly.scalacss" %%% "ext-scalatags" %  Versions.scalaCSS,

		"org.denigma" %%% "binding-controls" % Versions.bindingControls,

		"org.denigma" %%% "annotator" % Versions.annotator
	))

	//common purpose libs
	lazy val commonShared: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq(

		"io.circe" %%% "circe-core" % Versions.circe,

		"io.circe" %%% "circe-generic" % Versions.circe,

		"io.circe" %%% "circe-parser" % Versions.circe,

		"com.softwaremill.quicklens" %%% "quicklens" % Versions.quicklens,

		"com.lihaoyi" %%% "fastparse" % Versions.fastparse,

		"me.chrons" %%% "boopickle" % Versions.booPickle,

		"com.lihaoyi" %%% "pprint" % Versions.pprint,

		"com.lihaoyi" %% "sourcecode" % Versions.sourcecode,

		"io.github.soc" %%% "scala-java-time" % "2.0.0-M3"

	))

	val scientificJvm = Def.setting(Seq(
    "com.lihaoyi" %% "ammonite-ops" % Versions.ammonite,

		"org.sbolstandard" % "libSBOLj-parent" % Versions.libSBOLj,

		"org.scalanlp" %% "breeze" % Versions.breeze,

		"org.biopax.paxtools" % "paxtools" % Versions.paxtools

	
	))

	val otherJvm = Def.setting(Seq(
    "com.lihaoyi" %% "ammonite-ops" % Versions.ammonite,


		"com.iheart" %% "ficus" % Versions.ficus
	))

	/*
	val ammonite = Def.setting(Seq(
		"com.lihaoyi" %% "ammonite-ops" % Versions.ammonite,

		"com.lihaoyi" %% "ammonite-shell" % Versions.ammonite,

		"com.lihaoyi" %% "ammonite-repl" % Versions.ammonite % Test

	))
	*/


	val compilers = Def.setting(Seq(
		"org.scala-lang" % "scala-compiler" % Versions.scala
	))
}
