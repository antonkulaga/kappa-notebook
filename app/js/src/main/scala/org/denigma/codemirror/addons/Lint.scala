package org.denigma.codemirror.addons

import org.denigma.codemirror._

import scala.scalajs.js
import org.denigma.binding.extensions._
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.{JSName, ScalaJSDefined}
import scala.scalajs.js.{Array, JSON, |}
import scala.util.{Failure, Success}
import scalajs.concurrent.JSExecutionContext.Implicits.queue
@js.native
trait LintedConfiguration extends EditorConfiguration {
  var lint: Boolean | LintOptions = js.native
}

@ScalaJSDefined
class SyncLintOptions(lintFun: (String, LintOptions, Editor)=>js.Array[LintFound]) extends LintOptions(false)
{
  //document string, an options object, and an editor instance, return an array of {message, severity, from, to}

  def getAnnotations(text: String, options: LintOptions, cm: Editor): Array[LintFound] = {
    println("annotations work")
     lintFun(text, options, cm)
  }

}

/*
@ScalaJSDefined
class AsyncSyncLintOptions(lintFun: (String, LintOptions, Editor) => Future[js.Array[LintFound]]) extends LintOptions(true)
{
  //document string, an options object, and an editor instance, return an array of {message, severity, from, to}

  def getAnnotations(text: String, callback: js.Function2[Editor, js.Array[LintFound], Unit]): Unit= {
    lintFun(text, options, cm).onComplete{
      case Success(value) => callback(value)
      case Failure(th) => dom.console.error("cannot lint because of "+th)
    }
  }

}
*/
object LintOptions {

  def static(value: List[LintFound]): LintOptions = static(js.Array(value:_*))
  def static(value: js.Array[LintFound]): LintOptions = new SyncLintOptions(
    {
      case _ =>value
    }
  )

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


object Lint {

  val gutters: String = "CodeMirror-lint-markers"

  implicit def extendedConfiguration(config: EditorConfiguration): LintedConfiguration =
    config.asInstanceOf[LintedConfiguration]

  type LintHandler = js.Function1[String, js.Array[LintFound]]

  implicit class ExtendedCodemirror(obj: CodeMirror.type) {

    def addLint(mode: String)(fun: String=> js.Array[LintFound]): Unit = addLint(mode, fun)

    def addLint(mode: String, fun: LintHandler): Unit = {
      CodeMirror.dyn.registerHelper("lint", mode, fun)
    }

  }
}


