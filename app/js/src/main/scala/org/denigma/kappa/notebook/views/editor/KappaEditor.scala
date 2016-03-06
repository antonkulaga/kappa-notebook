package org.denigma.kappa.notebook.views.editor

import fastparse.all._
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.codemirror.{Editor, EditorChangeLike}
import org.denigma.codemirror.extensions._
import org.denigma.kappa.notebook.parsers.CommentLinksParser
import org.scalajs.dom.html._
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import rx.Var

import scala.scalajs.js
import scalatags.JsDom.all._


/**
  * Editor for the kappa code
  * @param elem HTML element for the view
  * @param code code for the editor
  * @param updates reactive varible to which we report our editor updates
  */
class KappaEditor(val elem: Element, val code: Var[String], val updates: Var[EditorUpdates]) extends BindableView with EditorView {


  override def mode = "Kappa"


  override def onChanges(ed: Editor, ch: js.Array[EditorChangeLike]): Unit = {
    updates() = EditorUpdates(Option(ed), ch.toList)
    val value = editor.getDoc().getValue()
    if(value!=code.now) code() = value

  }


  override def addEditor(name: String, element: ViewElement, codeMode: String): Unit = element match {
    case area: HTMLTextAreaElement =>
      //val text = if (area.value == "") defaultText else area.value
      editor = this.makeEditor(area, code.now, codeMode)
      code.foreach{ case str =>
          if(editor.getDoc().getValue()!=str) editor.getDoc().setValue(str)
      }
      editor

    case _ =>
      val message = "cannot find text area for the code!"
      throw new Exception(message)
  }
}
