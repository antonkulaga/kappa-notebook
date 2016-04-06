package org.denigma.kappa.notebook.views.editor

import fastparse.all._
import org.denigma.binding.binders.Events
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.codemirror._
import org.denigma.codemirror.extensions._
import org.denigma.kappa.notebook.KappaHub
import org.denigma.kappa.notebook.parsers.CommentLinksParser
import org.scalajs.dom.html._
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import rx._

import scala.scalajs.js
import scalatags.JsDom.all._
import rx.Ctx.Owner.Unsafe.Unsafe


/**
  * Editor for the kappa code
 *
  * @param elem HTML element for the view
  * @param hub ugly shareble hub to connect with other UI elements
  * @param updates reactive varible to which we report our editor updates
  */
class KappaEditor(val elem: Element, val hub: KappaHub, val updates: Var[EditorUpdates]) extends BindableView with EditorView {

  val errors = hub.errors
  val hasErrors = errors.map(_.nonEmpty)

  def code = hub.kappaCode

  override def mode = "Kappa"

  protected def onCursorActivity(ed: Editor) = {
    val c = doc.getCursor()
    val (prev, cur) = (hub.kappaCursor.now.line, c.line.toInt)
    if(prev != cur) {
      editor.addLineClass(cur, "background", "focused")
      editor.removeLineClass(prev, "background", "focused")
      hub.kappaCursor() = new PositionLike {override val line: Int = cur
        override val ch: Int = c.ch.toInt
      }
    }
  }


  override def onChanges(ed: Editor, ch: js.Array[EditorChangeLike]): Unit = {
    updates() = EditorUpdates(Option(ed), ch.toList)
    val value = doc.getValue()
    if(value!=code.now.text) code() = code.now.copy(text = value)
    //updateCursor()
  }


  override def addEditor(name: String, element: ViewElement, codeMode: String): Unit = element match {
    case area: HTMLTextAreaElement =>
      //val text = if (area.value == "") defaultText else area.value
      editor = this.makeEditor(area, code.now.text, codeMode)
      code.foreach{ case c =>
          if(doc.getValue()!=c.text) doc.setValue(c.text)
      }
      editor.addOnChanges(onChanges)
      val handler: (Editor) => Unit = onCursorActivity _
      editor.on("cursorActivity", handler)
      editor

    case _ =>
      val message = "cannot find text area for the code!"
      throw new Exception(message)
  }

  val onLeave = Var(Events.createMouseEvent())
  onLeave.triggerLater{
    updateCursor()
  }

  protected def updateCursor() = {
    val cur: Position = doc.getCursor()
    //println(hub.kappaCursor.now + " => "+doc.getCursor())
    hub.kappaCursor() = new PositionLike{
      override val ch: Int = cur.ch.toInt
      override val line: Int = cur.line.toInt
    }
  }

}
