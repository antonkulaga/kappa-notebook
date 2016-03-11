package org.denigma.kappa.notebook.views.editor
import fastparse.all._
import org.denigma.binding.binders.Events
import org.denigma.binding.commons.Uploader
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.codemirror.{Editor, EditorChangeLike}
import org.denigma.codemirror.extensions._
import org.denigma.kappa.WebSim.Code
import org.denigma.kappa.notebook.KappaHub
import org.denigma.kappa.notebook.parsers.CommentLinksParser
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html._
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import rx.Var
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.scalajs.js
import scala.util.{Failure, Success}
import scalatags.JsDom.all._
/**
  * Created by antonkulaga on 11/03/16.
  */
class SBOLEditor(val elem: Element, val hub: KappaHub, selected: Var[String],val updates: Var[EditorUpdates] ) extends BindableView with EditorView with Uploader{

  val active: rx.Rx[Boolean] = selected.map(value => value == this.id)

  def code = hub.sbolCode

  override def mode = "sbol"

  val generate = Var(Events.createMouseEvent())
  generate.triggerLater{
    println("it should generate something")
  }

  val onUpload: Var[Event] = Var(Events.createEvent())
  onUpload.triggerLater(
    onUpload.onChange(ev =>
      this.uploadHandler(ev){
        case Success((file, text))=> code() = code.now.copy(text)
        case Failure(th) => dom.console.error(s"File upload failure: ${th.toString}")
      })
  )


  override def onChanges(ed: Editor, ch: js.Array[EditorChangeLike]): Unit = {
    updates() = EditorUpdates(Option(ed), ch.toList)
    val value = doc.getValue()
    if(value!=code.now.text) code() = code.now.copy(text = value)

  }


  override def addEditor(name: String, element: ViewElement, codeMode: String): Unit = element match {
    case area: HTMLTextAreaElement =>
      //val text = if (area.value == "") defaultText else area.value
      editor = this.makeEditor(area, code.now.text, codeMode)
      code.foreach{ case c =>
        if(doc.getValue()!=c.text) doc.setValue(c.text)
      }

      editor

    case _ =>
      val message = "cannot find text area for the code!"
      throw new Exception(message)
  }

}
