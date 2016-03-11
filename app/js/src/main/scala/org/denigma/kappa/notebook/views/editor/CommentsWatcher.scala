package org.denigma.kappa.notebook.views.editor

import fastparse.all._
import org.denigma.codemirror.Editor
import org.denigma.codemirror.extensions._
import org.denigma.kappa.notebook.parsers.CommentLinksParser
import org.scalajs.dom.html._
import rx._

import scalatags.JsDom.all._
/**
  * Created by antonkulaga on 11/03/16.
  */
class CommentsWatcher(updates: Var[EditorUpdates])  {

  //val linkParser = P( "a" )
  val commentsParser = new CommentLinksParser().linkAfterComment

  protected def searchForLinks(editor: Editor, line: String, num: Int) = {
    commentsParser.parse(line) match {
      case Parsed.Success(result, index) =>
        val marker = this.makeMarker(result)
        editor.setGutterMarker(num, "breakpoints", marker)

      case Parsed.Failure(parser, index, extra) =>
        editor.setGutterMarker(num, "breakpoints", null) //test setting null
    }
  }


  protected def makeMarker(link: String): Anchor = {
    val tag = a(href := link,
      i(`class` := "file pdf outline icon")
    )
    tag.render

    // <i class="file pdf outline icon">
  }


  protected def changeHandler(upd: EditorUpdates) =
  for {
    editor <- upd.editorOpt
    changed = upd.updates
  }
  {
    val (from, to) = changed.foldLeft( (Int.MaxValue, 0)) {
      case (acc, ch) => ch.mergeSpans(acc)
    }
    val lines = from to to
    //if(lines.nonEmpty)  editor.clearGutter("breakpoints")
    for {
      (num, line) <- editor.linesText(lines)
    } {
      searchForLinks(editor, line , num)
      //val info: LineInfo = editor.lineInfo(num)
      //val markers: js.Array[String] = info.gutterMarkers
    }
  }
}
