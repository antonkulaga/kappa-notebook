package org.denigma.codemirror.addons.lint

import org.denigma.binding.extensions._
import org.denigma.codemirror._
import org.denigma.kappa.messages.WebSimMessages.WebSimError

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSName, ScalaJSDefined}
import scala.scalajs.js.{Array, |}

@js.native
trait LintedConfiguration extends EditorConfiguration {
  var lint: Boolean | LintOptions = js.native
}

@ScalaJSDefined
class StaticLintOptions(value: js.Array[LintFound]) extends LintOptions(false)
{
  def getAnnotations(text: String, options: LintOptions, cm: Editor): Array[LintFound] = {
    println("static annotations work")
    value
  }
}

@ScalaJSDefined
class SyncLintOptions(lintFun: (String, LintOptions, Editor) => js.Array[LintFound]) extends LintOptions(false)
{
  //document string, an options object, and an editor instance, return an array of {message, severity, from, to}

  def getAnnotations(text: String, options: LintOptions, cm: Editor): js.Array[LintFound] = {
    println("annotations work")

    lintFun(text, options, cm)
  }
}

object LintOptions {

  def static(value: List[LintFound]): LintOptions = static(js.Array(value:_*))

  def static(value: js.Array[LintFound]): LintOptions = {
    def handler(st: String, opt: LintOptions, ed: Editor): Array[LintFound] = value
    new SyncLintOptions(handler)
  }

}
@ScalaJSDefined
class LintOptions(val async: Boolean) extends js.Object


object LintFound {
  implicit def fromWebSimError(error: WebSimError): LintFound = {
    val from = new PositionLike {
      override val line: Int = error.range.from_position.line
      override val ch: Int = error.range.from_position.chr
    }
    val to =  new PositionLike {
      override val line: Int = error.range.to_position.line
      override val ch: Int = error.range.to_position.chr
    }
    new LintFound(from, to, error.severity, error.message)
  }
}

@ScalaJSDefined
class LintFound(val from: PositionLike, val to: PositionLike, val severity: String, val message: String) extends js.Object