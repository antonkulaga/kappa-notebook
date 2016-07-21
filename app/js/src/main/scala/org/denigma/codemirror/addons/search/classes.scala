package org.denigma.codemirror.addons.search

import org.denigma.binding.extensions._
import org.denigma.codemirror._
import org.denigma.kappa.messages.WebSimMessages.WebSimError

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSName, ScalaJSDefined}
import scala.scalajs.js.{Array, |}

@js.native
trait SearchConfiguration extends EditorConfiguration {
  //var extraKeys: js.Dictionary[String] = js.native
}
/*
@ScalaJSDefined
class StaticLintOptions(value: js.Array[LintFound]) extends LintOptions(false)
{
  def getAnnotations(text: String, options: LintOptions, cm: Editor): Array[LintFound] = {
    println("static annotations work")
    value
  }
}*/