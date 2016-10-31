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
      override val line: Int = error.range.map(r=>r.from_position.line).getOrElse(-1)
      override val ch: Int = error.range.map(r=>r.from_position.chr).getOrElse(-1)
    }
    val to =  new PositionLike {
      override val line: Int = error.range.map(r=>r.to_position.line).getOrElse(-1)
      override val ch: Int = error.range.map(r=>r.to_position.chr).getOrElse(-1)
    }
    new LintFound(from, to, error.severity, error.message)
  }
}

@ScalaJSDefined
class LintFound(val from: PositionLike, val to: PositionLike, val severity: String, val message: String) extends js.Object