package org.denigma.codemirror.addons


import org.denigma.binding.extensions._
import org.denigma.codemirror._
import org.denigma.kappa.messages.WebSimMessages.WebSimError

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSName, ScalaJSDefined}
import scala.scalajs.js.{Array, |}


package object hint {


  implicit def extendedConfiguration(config: EditorConfiguration): HintConfiguration =
    config.asInstanceOf[HintConfiguration]

  /*
  type HintHandler = js.Function1[String, js.Array[LintFound]]

  implicit class ExtendedCodeMirror(obj: CodeMirror.type) {

    def addHint(mode: String)(fun: String=> js.Array[LintFound]): Unit = addLint(mode, fun)

    def addLint(mode: String, fun: HintHandler: Unit = {
      CodeMirror.dyn.registerHelper("lint", mode, fun)
    }

  }
  */
}
