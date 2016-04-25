package org.denigma.kappa.notebook.views.editor

import fastparse.all._
import org.denigma.codemirror.Editor
import org.denigma.codemirror.extensions._
import org.denigma.controls.papers.Bookmark
import org.denigma.kappa.notebook.KappaHub
import org.denigma.kappa.notebook.parsers.{CommentLinksParser, PaperParser}
import org.scalajs.dom.html.Anchor
import org.scalajs.dom.raw.MouseEvent
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scalatags.JsDom.all._
/**
  * Created by antonkulaga on 11/03/16.
  */
class CommentsWatcher(updates: Var[EditorUpdates], hub: KappaHub)  {

  def papers = hub.papers
  updates.foreach(changeHandler) //subscription

  val commentsParser = new CommentLinksParser()
  def linkParser = commentsParser.link
  val paperParser = new PaperParser()

  protected def searchForLinks(editor: Editor, line: String, num: Int) = {
    linkParser.parse(line) match {
      case Parsed.Success(result, index) =>
        //println("LINK FOUND!!!!")
        val marker = this.makeURIMarker(result)
        editor.setGutterMarker(num, "breakpoints", marker)

      case Parsed.Failure(parser, index, extra) =>
        //editor.setGutterMarker(num, "breakpoints", null) //test setting null
    }
  }

  protected def mergeComments(num: Int, ed: Editor, text: List[(Int,String)] = Nil): List[(Int, String)]  = if(num >= 0)  {
    val line = ed.getDoc().getLine(num)
    if(line.replace(" ","").replace("\t", "")=="") mergeComments(num -1, ed, text) else {
      commentsParser.comment.parse(line) match {
        case Parsed.Success(comm, _) => mergeComments(num -1, ed, (num, comm)::text)
        case f: Parsed.Failure => text.reverse
      }
    }
  } else text.reverse

  protected def searchInComments(editor: Editor, num: Int) = {
    val comments: List[(Int, String)] = mergeComments(num , editor)
    //println("search comments works, COMMENTS are: \n" + comments.mkString("\n"))
    searchForPages(editor, comments, num)
  }

  protected def searchForPages(editor: Editor, lines: List[(Int, String)], currentNum: Int) = {
    val pages: List[((Int, String), Int)] = lines.map{ case (num, line)=> (num, line) -> paperParser.page.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }
    val papers = lines.map{ case (num, line)=> (num, line) -> paperParser.paper.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }
    val links = lines.map{ case (num, line)=> (num, line) -> linkParser.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }
    links.collectFirst{
      case ((n, line), result) if n == currentNum =>
        val marker = makeURIMarker(result)
        editor.setGutterMarker(n, "breakpoints", marker)
    }

    /*
    for( (paper, page) <- papers.zip(pages.map(_.))){
      val marker = makePageMarker(paper, page)
      editor.setGutterMarker(, "breakpoints", marker)
    }
    */
    //TODO: fix this bad code
    if(papers.nonEmpty) {
      val paper: String = papers.lastOption.map{ case (key, v)=>v}.get
      val pg: Int = if(pages.nonEmpty) pages.lastOption.map(_._2).getOrElse(1) else 1
      val marker = this.makePageMarker(paper, pg)
      editor.setGutterMarker(currentNum, "breakpoints", marker)
    }
  }

  protected def makeURIMarker(link: String): Anchor = {
    val tag = a(href := link,
      i(`class` := "link outline icon")
    )
    //println("TAG IS "+tag)
    tag.render
  }

  protected def makePageMarker(paper: String, num: Int) = {
    val tag = button(`class` := "ui icon button", i(`class` := "file pdf outline icon", onclick := {
      //println(s"mouse down on $num")

      /*
      for( pap <- papers.now.keySet.collectFirst{ case p if p.contains(paper) => p} )
        {

        }
      location() = location.now.copy(page = num)
      */
      }))
    val html = tag.render
    html.onclick = {
      event: MouseEvent => //println(s"click on $num")
          println("papers work")
        //location() = location.now.copy(page = num)
    }
    html
  }

  protected def makeImageMarker(src: String) = {
    val tag = button(`class` := "ui icon button", i(`class` := "file image outline icon", onclick := {
      //println(s"mouse down on $num")
    }))
    val html = tag.render
    html.onclick = {
      event: MouseEvent =>
        hub.selectedImage() = src
    }
    html
  }


  protected def changeHandler(upd: EditorUpdates) =
  {
    for {
      (editor, changedLines) <- upd.changedLinesOpt
      (num, line) <- changedLines
    } {

      searchInComments(editor, num)
    }
  }
}
