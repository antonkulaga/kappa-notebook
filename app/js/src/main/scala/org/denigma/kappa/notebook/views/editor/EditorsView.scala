package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.ReactiveBinder
import org.denigma.binding.views.BindableView
import org.denigma.codemirror.extensions.{EditorConfig, _}
import org.denigma.codemirror.{CodeMirror, Editor, _}
import org.scalajs.dom
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import rx.Var

import scala.scalajs.js

object EditorUpdates {

  lazy val empty = EditorUpdates(None, Nil)

}

/**
  * Class to follow to Editor updates
  * @param editorOpt
  * @param updates
  */
case class EditorUpdates(editorOpt: Option[Editor], updates: List[EditorChangeLike])
{

  lazy val changedLinesOpt: Option[(Editor, Seq[(Int, String)])] =
    for {
      editor <- editorOpt
      changed = updates
    }
      yield {
        val (from, to) = changed.foldLeft( (Int.MaxValue, 0)) {
          case (acc, ch) => ch.mergeSpans(acc)
        }
        val lines = from to to
        editor -> editor.linesText(lines)
      }
}

trait EditorView extends BindableView with EditorMaker with WithMirrors{

  self=>

  def mode: String = "Kappa"

  def doc: Doc = editor.getDoc()

  def editorUpdates: Var[EditorUpdates] //used to subscribe editor to changes


  protected var _editor: Editor = null
  def editor: Editor = {
    if (_editor == null) dom.console.error("editor is NULL!")
    _editor
  }

  def editor_=(value: Editor): Unit = {
    _editor = value
    subscribeEditor(_editor)
  }

  def onChanges(ed: Editor, ch: js.Array[EditorChangeLike]): Unit = {
    editorUpdates() = EditorUpdates(Option(ed), ch.toList)
  }

  protected def subscribeEditor(editor: Editor) = {

    def onGutterClick(ed: Editor, line: Int): Unit = {
      gutterClicks() = line
    }
    //editor.addOnChange(onChange)
    editor.addOnGutterClick(onGutterClick)
    editor.addOnChanges(onChanges)
  }

  lazy val gutterClicks: Var[Int] = Var(0)

  def contains(name: String): Boolean = if (_editor == null) false else {
    dom.console.error(s"warning: EditorView($name) already contains an editor")
    true
  }

  withBinder(v=>new EditorsBinder(v, mode))

}


trait WithMirrors extends BindableView {

  def contains(name: String): Boolean

  def addEditor(name: String, element: Element, codeMode: String): Unit

}


trait EditorMaker {

  def makeEditor(area: HTMLTextAreaElement, textValue: String, codeMode: String, readOnly: Boolean = false): Editor = {
    val params = EditorConfig
      .mode(codeMode)
      .lineNumbers(true)
      .value(textValue)
      .readOnly(readOnly)
      .viewportMargin(Integer.MAX_VALUE)
      .gutters(js.Array("CodeMirror-lint-markers", "CodeMirror-linenumbers", "breakpoints"))
    //  gutters: ["CodeMirror-linenumbers", "breakpoints"]
    CodeMirror.fromTextArea(area, params)
  }

}

class EditorsBinder(view: WithMirrors, defaultMode: String = "htmlmixed") extends ReactiveBinder
{
  override def bindAttributes(el: Element, attributes: Map[String, String]): Boolean= {
    val ats = this.dataAttributesOnly(attributes)
    val fun: PartialFunction[(String, String), Unit] = elementPartial(el, ats).orElse{ case other =>}
    ats.foreach(fun)
    true
  }

  override def elementPartial(el: Element, ats: Map[String, String]): PartialFunction[(String, String), Unit] = {
    case ("editor", v) if !view.contains(v) =>
      view.addEditor(v, el, ats.getOrElse("mode", defaultMode))
  }
}