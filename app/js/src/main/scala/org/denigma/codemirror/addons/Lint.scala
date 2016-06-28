package org.denigma.codemirror.addons

import org.denigma.codemirror.{Position, CodeMirror, Editor, EditorConfiguration}

import scala.scalajs.js
import org.denigma.binding.extensions._

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.ScalaJSDefined

@js.native
trait LintedConfiguration extends EditorConfiguration {
  var lint: Boolean = js.native
}

object Lint {

  val gutters: String = "CodeMirror-lint-markers"

  implicit def extendedConfiguration(config: EditorConfiguration): LintedConfiguration =
    config.asInstanceOf[LintedConfiguration]

  type LintHandler = js.Function1[String, js.Array[Found]]

  @ScalaJSDefined
  class Found(val from: Position, val to: Position, val message: String) extends js.Object

  implicit class ExtendedCodemirror(obj: CodeMirror.type) {

    def addLint(mode: String)(fun: String=> js.Array[Found]): Unit = addLint(mode, fun)

    def addLint(mode: String, fun: LintHandler): Unit = {
      CodeMirror.dyn.registerHelper("lint", mode, fun)
    }

  }
}


