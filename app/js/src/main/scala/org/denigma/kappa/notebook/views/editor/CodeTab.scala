package org.denigma.kappa.notebook.views.editor

import fastparse.all._
import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.commons.Uploader
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, UpdatableView}
import org.denigma.codemirror._
import org.denigma.codemirror.extensions._
import org.denigma.kappa.messages.KappaFile
import org.denigma.kappa.notebook.views.common.TabItem
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import rx._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.denigma.binding.extensions._
import scala.util._
import scala.scalajs.js
import rx.Ctx.Owner.Unsafe.Unsafe

class CodeTab(val elem: Element,
              name: String,
              val source: Var[KappaFile],
              val selected: Var[String],
              val editorUpdates: Var[EditorUpdates],
              val kappaCursor: Var[Option[(Editor, PositionLike)]]
             )  
  extends BindableView 
    with EditorView 
    with Uploader 
    with UpdatableView[KappaFile]
    with TabItem
{

  override lazy val id: String = name

  //override def id = name


  override def mode = "Kappa"

  val code = Var(source.now.content)
  code.onChange{
    case str =>
      if(source.now.content!=str) source() = source.now.copy(content = str)
  }
  //val code = source.map(s=>s.content)

  override def update(value: KappaFile): this.type = {
    source() = value
    this
  }

  override def bindView: Unit ={
    if(active.now==false) {
      active.
    }
    super.bindView()
  }

  override def unbindView() = {
    super.unbindView()
    source.kill()
  }
  
  protected def onCursorActivity(ed: Editor) = {
    val c = doc.getCursor()
    val cur = c.line.toInt
    kappaCursor.now match {
      case None=>
        editor.addLineClass(cur, "background", "focused")
        kappaCursor() = Some(editor, new PositionLike {override val line: Int = cur
          override val ch: Int = c.ch.toInt
        })
      case Some((e, prev)) if prev.line !=cur || prev.ch != c.ch.toInt || e != ed =>
        editor.addLineClass(cur, "background", "focused")
        editor.removeLineClass(prev, "background", "focused")
        kappaCursor() = Some(editor, new PositionLike {override val line: Int = cur
          override val ch: Int = c.ch.toInt
        })
      case _ => //do nothing
    }
  }
  

  override def addEditor(name: String, element: ViewElement, codeMode: String): Unit = element match {
    case area: HTMLTextAreaElement =>
      println("let us add editor!")
      //val text = if (area.value == "") defaultText else area.value
      editor = this.makeEditor(area, code.now, codeMode)
      code.foreach{ case text =>
        if(doc.getValue()!=text) doc.setValue(text)
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

  val save = Var(Events.createMouseEvent())
  save.triggerLater{
    //saveAs(name, code.now)
  }

  val onUpload: Var[Event] = Var(Events.createEvent())
  onUpload.onChange(ev =>
    this.uploadHandler(ev){
      case Success((file, text))=>
        //hub.name() = file.name
        //hub.runParameters.set(hub.runParameters.now.copy(fileName = file.name))
        //code.set(text)
        //kappaCode() = kappaCode.now.copy(text = text)
      case Failure(t) => dom.console.error(s"File upload failure: ${t.toString}")
    })

  override def onChanges(ed: Editor, ch: js.Array[EditorChangeLike]): Unit = {
    editorUpdates() = EditorUpdates(Option(ed), ch.toList)
    val value = doc.getValue()
    if(value != code.now) code() = value
    //updateCursor()
  }

  protected def updateCursor() = {
    val cur: Position = doc.getCursor()
    kappaCursor() = Some(this.editor -> new PositionLike{
      override val ch: Int = cur.ch.toInt
      override val line: Int = cur.line.toInt
    })
  }
}
