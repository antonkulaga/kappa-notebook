package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.Events
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.codemirror._
import org.denigma.codemirror.addons.lint._
import org.denigma.codemirror.extensions._
import org.denigma.kappa.messages._
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.notebook.views.common.TabItem
import org.scalajs.dom
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import rx.Ctx.Owner.Unsafe.Unsafe

import scalajs.js.JSConverters._
import rx._

import scala.concurrent.duration._
import scala.scalajs.js

class KappaCodeTab(elem: Element,
                   source: Var[KappaSourceFile],
                   selected: Var[String],
                   val input: Var[KappaMessage],
                   val output: Var[KappaMessage],
                   editorUpdates: Var[EditorUpdates],
                   kappaCursor: Var[KappaCursor],
                   val errors: Rx[List[WebSimError]]
             ) extends CodeTab(elem, source, selected, editorUpdates: Var[EditorUpdates], kappaCursor: Var[KappaCursor])
{
  override def mode = "Kappa"


  input.onChange(onInputChange)

  errors.onChange{ ers=>
    //dom.console.error(s"CodeTab $name ERRORS: "+ers.mkString("\n"))
    val found: List[LintFound] = ers.map{e=> e:LintFound}
    def gts(text: String, options: LintOptions, cm: Editor): js.Array[LintFound] = {
      found.toJSArray
    }
    val fun: js.Function3[String, LintOptions, Editor, js.Array[LintFound]] = gts _

    editor.setOption("lint", js.Dynamic.literal(
      getAnnotations = fun
    ))
  }


  lazy val updateAfter = 800 millis

  val delayedCode = code.afterLastChange(updateAfter){
   value =>
     val file = source.now.copy(saved = false)
     //println("SOURCE UPDATE: "+file.path)
     input() = FilesUpdate.updatedFiles(file)
  }

}
