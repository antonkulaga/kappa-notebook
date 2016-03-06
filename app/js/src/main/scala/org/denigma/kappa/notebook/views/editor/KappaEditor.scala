package org.denigma.kappa.notebook.views.editor

import fastparse.all._
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.codemirror.EditorChangeLike
import org.denigma.codemirror.extensions._
import org.scalajs.dom.html._
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import rx.Var

import scala.scalajs.js
import scalatags.JsDom.all._

/**
  * Created by antonkulaga on 01/03/16.
  */
class KappaEditor(val elem: Element, val code: Var[String]) extends BindableView with EditorView {

  //val linkParser = P( "a" )
  val commentsParser = new CommentsParser().linkAfterComment


  override def mode = "Kappa"

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

  protected def makeMarker(link: String): Anchor = {
    val tag = a(href := link,
      i(`class` := "file pdf outline icon")
    )
    tag.render

    // <i class="file pdf outline icon">
  }

  protected def changeHandler(changed: js.Array[EditorChangeLike]) = {
    val (from, to) = changed.foldLeft( (Int.MaxValue, 0)) {
      case (acc, ch) => ch.mergeSpans(acc)
    }
    val lines = from to to
    //if(lines.nonEmpty)  editor.clearGutter("breakpoints")
    for {
      (num, line) <- editor.linesText(lines)
    } {
      //val info: LineInfo = editor.lineInfo(num)
      //val markers: js.Array[String] = info.gutterMarkers
      commentsParser.parse(line) match {
        case Parsed.Success(result, index) =>
          val marker = this.makeMarker(result)
          editor.setGutterMarker(num, "breakpoints", marker)

        case Parsed.Failure(parser, index, extra) =>
          editor.setGutterMarker(num, "breakpoints", null) //test setting null
      }
    }
    val value = editor.getDoc().getValue()
    if(value!=code.now) code() = value
  }

  changes.onChange(changeHandler)

  gutterClicks.onChange{
    case line =>
    //  val marker = this.makeMarker("https://codemirror.net/demo/marker.html")
    //    editor.setGutterMarker(line, "breakpoints", marker)

  }
}
