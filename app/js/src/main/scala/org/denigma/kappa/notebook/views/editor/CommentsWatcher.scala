package org.denigma.kappa.notebook.views.editor

import fastparse.all._
import org.denigma.codemirror.Editor
import org.denigma.codemirror.extensions._
import org.denigma.controls.papers.Bookmark
import org.denigma.kappa.notebook.parsers.{CommentLinksParser, ImageParser, PaperParser}
import org.scalajs.dom.html.Anchor
import org.scalajs.dom.raw.MouseEvent
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scalatags.JsDom.all._
/**
  * Created by antonkulaga on 11/03/16.
  */
class CommentsWatcher(
                       val updates: Var[EditorUpdates],
                       val location: Var[Bookmark] )  {

  updates.foreach(changeHandler) //subscription

  val commentsParser = new CommentLinksParser()
  def linkParser = commentsParser.link
  val paperParser = new PaperParser()
  val imageParser = new ImageParser()

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
    semanticSearch(editor, comments, num)
  }

  protected def semanticSearch(editor: Editor, lines: List[(Int, String)], currentNum: Int) = {
    //for( (i, str) <- lines)  editor.setGutterMarker(i,  "breakpoints", null)

    val pages: List[((Int, String), Int)] = lines.map{ case (num, line)=> (num, line) -> paperParser.page.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }
    val papers = lines.map{ case (num, line)=> (num, line) -> paperParser.paper.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }
    val links = lines.map{ case (num, line)=> (num, line) -> linkParser.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }

    val images = lines.map{ case (num, line)=> (num, line) -> imageParser.image.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }
    /*
    val videos = lines.map{ case (num, line)=> (num, line) -> videosParser.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }
    */

    links.collectFirst{
      case ((n, line), result) if n == currentNum =>
        val marker = makeURIMarker(result)
        editor.setGutterMarker(n, "breakpoints", marker)
    }
    images.collectFirst{
      case ((n, line), result) if n == currentNum =>
        val marker = makeImageMarker(result)
        editor.setGutterMarker(n, "breakpoints", marker)
    }

    papers.collectFirst{
      case ((n, line), result) if n == currentNum =>
        val marker = makePageMarker(result)
        editor.setGutterMarker(n, "breakpoints", marker)
    }
    /*
    //TODO: fix this bad code
    if(papers.nonEmpty) {
      val paper: String = papers.collect{case ((n, line), result) if n == currentNum=> ((n, line), result)}.lastOption.map{ case (key, v)=>v }.get
      val pg: Int = if(pages.nonEmpty) pages.collect{case ((n, line), result) if n == currentNum=> ((n, line), result)}.lastOption.map(_._2).getOrElse(1) else 1
      val marker = this.makePageMarker(paper, pg)
      editor.setGutterMarker(currentNum, "breakpoints", marker)
    }
    */
  }

  protected def makeURIMarker(link: String): Anchor = {
    val tag = a(href := link, target := "blank",
      i(`class` := "label File Code Outline large icon", id :="class_icon"+Math.random()),
      " "
    )
    tag.render
  }

  protected def makeImageMarker(image: String) = {
    val tag = button(`class` := "ui icon tiny button", i(`class` := "label File Code Outline large icon", onclick := {
      //println(s"mouse down on $num")
    }))
    val html = tag.render
    html.onclick = {
      event: MouseEvent =>
        println("image = "+ image.trim)
        /*
        println("SELECTOR = "+selector.image)
        println("SELECOT == "+selector.image==image.trim)
        selector.image() = image.trim
        selector.go2images()
        */
    }
    html
  }

  protected def makePageMarker(paper: String) = {
    val tag = button(`class` := "ui icon tiny button", i(`class` := "label File Code Outline large icon", onclick := {
      //println(s"mouse down on $num")
      }))
    val html = tag.render
    html.onclick = {
      event: MouseEvent => //println(s"click on $num")
        //  println("papers work")
        //location() = location.now.copy(page = num)
        //selector.paper() =
        /*
        println("paper"+ paper)
        selector.paper() = paper
        selector.go2papers()
        */
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
