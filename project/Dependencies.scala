import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

case class CrossDep(
											 shared: Def.Initialize[Seq[ModuleID]],
											 jvm: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq.empty[ModuleID]),
											 js: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq.empty[ModuleID]))

object Dependencies {

	//libs for testing
  lazy val testing = Def.setting(Seq(
		"org.scalatest" %%% "scalatest" % Versions.scalaTest
  ))

	//akka-related libs
	lazy val akka = Def.setting(Seq(

		"org.denigma" %%% "akka-http-extensions" % Versions.akkaHttpExtensions,

		"com.typesafe.akka" %% "akka-http-testkit-experimental" % Versions.akkaHttp
	))

	lazy val meta = CrossDep(
		shared = Def.setting(Seq(			"com.lihaoyi" %%% "scalarx" % Versions.scalaRx)),

		jvm = Def.setting(Seq.empty),

		js = Def.setting(Seq.empty)
	)


	//scalajs libs
	lazy val sjsLibs= Def.setting(Seq(
		"org.scala-js" %%% "scalajs-dom" % Versions.dom,

		"org.querki" %%% "jquery-facade" % Versions.jqueryFacade, //scalajs facade for jQuery + jQuery extensions

		"org.denigma" %%% "codemirror-facade" % Versions.codemirrorFacade,

		"org.denigma" %%% "semantic-ui-facade" % Versions.semanticUIFacade,

		"org.denigma" %%% "threejs-facade" % Versions.threejsFacade
	))

	//dependencies on javascript libs
	lazy val webjars= Def.setting(Seq(
		"org.webjars" % "Semantic-UI" %  Versions.semanticUI,

		"org.webjars" % "codemirror" % Versions.codemirror,

		"org.webjars" % "jquery" % Versions.jquery,

		"org.webjars" % "three.js" % Versions.threeJS
	))

	//common purpose libs
	lazy val commonShared: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq(
		"com.github.japgolly.scalacss" %%% "core" % Versions.scalaCSS,

		"com.github.japgolly.scalacss" %%% "ext-scalatags" %  Versions.scalaCSS,

		"org.denigma" %%% "binding-controls" % Versions.bindingControls,

		"com.lihaoyi" %% "fastparse" % Versions.fastparse

	))



	val otherJvm = Def.setting(Seq(
		"me.lessis" %% "retry" % Versions.retry,

    "com.lihaoyi" %% "ammonite-ops" % Versions.ammonite
	))


	val compilers = Def.setting(Seq(
		"org.scala-lang" % "scala-compiler" % Versions.scala
	))
}
