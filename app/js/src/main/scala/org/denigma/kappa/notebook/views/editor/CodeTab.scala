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


class CodeTab(val elem: Element,
              val source: Var[KappaSourceFile],
              val selected: Var[String],
              val editorUpdates: Var[EditorUpdates],
              val kappaCursor: Var[KappaCursor])  extends BindableView
  with EditorView
  with TabItem{

  active.onChange{
    case true =>
    //if(this._editor!=null) editor.refresh()

    case false =>
      elem.style.display = "none"
    //println("active value for "+id+" is false and display is "+elem.style.display)
  }

  lazy val name = source.map(s=>s.name)

  lazy val path = source.map(s=>s.path)

  lazy val wrapLines: Var[Boolean] = Var(false)

  override lazy val id: String = path.now

  val notSaved = source.map(s => !s.saved)

  val code = Var(source.now.content)
  code.onChange{ str =>
    if(source.now.content!=str) source() = source.now.copy(content = str, saved = false)
  }

  override def makeEditor(area: HTMLTextAreaElement, textValue: String, codeMode: String, readOnly: Boolean = false): Editor = {
    val params: EditorConfigurationBuilder = EditorConfig
      .mode(codeMode)
      .lineNumbers(true)
      .value(textValue)
      .readOnly(readOnly)
      .viewportMargin(Integer.MAX_VALUE)
      .extraKeys(js.Dictionary( ("Alt-F", "findPersistent")))
      .gutters(js.Array(gutters, "CodeMirror-linenumbers", "breakpoints"))
      .lineWrapping(wrapLines.now)

    val config: EditorConfiguration = params
    //config.dyn.scrollbarStyle = null
    CodeMirror.fromTextArea(area, config)
  }


  override def addEditor(name: String, element: ViewElement, codeMode: String): Unit = element match {
    case area: HTMLTextAreaElement =>
      editor = this.makeEditor(area, code.now, codeMode)
      code.foreach{  text =>
        if(doc.getValue()!=text) doc.setValue(text)
      }
      editor.addOnChanges(onChanges)
      val handler: (Editor) => Unit = onCursorActivity
      editor.on("cursorActivity", handler)
      if(active.now && kappaCursor.now != EmptyCursor) onCursorActivity(editor)
      editor

    case _ =>
      val message = "cannot find text area for the code!"
      throw new Exception(message)
  }


  protected def onInputChange(message: KappaMessage): Unit = message match {
      case Go.ToSource(p, from ,to) if p.value == path.now | p.local ==path.now | p.local == name.now | (p.value == "" && active.now) =>
        println(s"from ${from} to ${to}")
        editor.getDoc().setCursor(js.Dynamic.literal(line = from, ch = 1).asInstanceOf[Position])

      case Go.ToSource(p, from ,to) if p.local == name.now | (p.value == "" && active.now) =>
        println(s"from ${from} to ${to}")
        editor.getDoc().setCursor(js.Dynamic.literal(line = from, ch = 1).asInstanceOf[Position])

      case Go.ToFile(s: KappaSourceFile) if path.now == s.path =>
        source() = s
        code() = s.content
        selected() = s.path

      case _=> //do nothing
  }


  override def onChanges(ed: Editor, ch: js.Array[EditorChangeLike]): Unit = {
    editorUpdates() = EditorUpdates(Option(ed), ch.toList)
    val value = doc.getValue()
    if(value != code.now) code() = value

    //editor.setOption("lint", true) //trigger linting
    //println("lint = true")
    updateCursor()
  }



  protected def onCursorActivity(ed: Editor) = {
    val c = doc.getCursor()
    kappaCursor.now match {
      case EmptyCursor =>
        editor.addLineClass(c.line, "background", "focused")
        kappaCursor() = KappaEditorCursor(source.now, editor, c.line, c.ch)

      case KappaEditorCursor(fl, editor, prevLine, prevCh) if prevLine != c.line || prevCh != c.ch || editor != ed =>
        editor.addLineClass(c.line, "background", "focused")
        editor.removeLineClass(prevLine, "background", "focused")

        kappaCursor() = KappaEditorCursor(source.now, ed, c.line, c.ch)

      case _ => //do nothing
    }
  }


  val onLeave = Var(Events.createMouseEvent())
  onLeave.triggerLater{
    updateCursor()
  }

  override def unbindView() = {
    super.unbindView()
    source.kill()
  }


  protected def updateCursor() = {
    val cur: Position = doc.getCursor()
    kappaCursor() = KappaEditorCursor(source.now, editor, cur.line, cur.ch)
  }

}
