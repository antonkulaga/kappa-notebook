package org.denigma.kappa.notebook.views.editor

import fastparse.all._
import org.denigma.codemirror.Editor
import org.denigma.codemirror.extensions._
import org.denigma.controls.papers.Bookmark
import org.denigma.kappa.notebook.parsers.CommentLinksParser
import org.scalajs.dom.html.Anchor
import org.scalajs.dom.raw.MouseEvent
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scalatags.JsDom.all._
/**
  * Created by antonkulaga on 11/03/16.
  */
class CommentsWatcher(updates: Var[EditorUpdates], papers: Var[Map[String, Bookmark]])  {

  updates.foreach(changeHandler) //subscription

  val commentsParser = new CommentLinksParser()
  val linkParser = commentsParser.linkAfterComment
  val pageParser =  commentsParser.page

  protected def searchForLinks(editor: Editor, line: String, num: Int) = {
    linkParser.parse(line) match {
      case Parsed.Success(result, index) =>
        val marker = this.makeURIMarker(result)
        editor.setGutterMarker(num, "breakpoints", marker)

      case Parsed.Failure(parser, index, extra) =>
        editor.setGutterMarker(num, "breakpoints", null) //test setting null
    }
  }

  protected def searchForPages(editor: Editor, line: String, num: Int) = {
    pageParser.parse(line) match {
      case Parsed.Success(page, index) =>
        //val marker = this.makePageMarker(page)
        //editor.setGutterMarker(num, "breakpoints", marker)

      case Parsed.Failure(parser, index, extra) =>
        editor.setGutterMarker(num, "breakpoints", null) //test setting null
    }
  }

  protected def makeURIMarker(link: String): Anchor = {
    val tag = a(href := link,
      i(`class` := "link outline icon")
    )
    tag.render

    // <i class="file pdf outline icon">
  }
/*
  protected def makePageMarker(num: Int) = {
    val tag = button(`class` := "ui icon button", i(`class` := "file pdf outline icon", onclick := {
      //println(s"mouse down on $num")
      location() = location.now.copy(page = num)
      }))
    val html = tag.render
    html.onclick = {
      event: MouseEvent => //println(s"click on $num")
        location() = location.now.copy(page = num)
    }
    html
  }
*/


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
      //searchForPages(editor, line, num)
      //val info: LineInfo = editor.lineInfo(num)
      //val markers: js.Array[String] = info.gutterMarkers
    }
  }
}
