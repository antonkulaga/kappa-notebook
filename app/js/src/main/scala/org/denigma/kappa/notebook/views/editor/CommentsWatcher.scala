package org.denigma.kappa.notebook.views.editor

import fastparse.all._
import org.denigma.codemirror.Editor
import org.denigma.codemirror.extensions._
import org.denigma.controls.papers.Bookmark
import org.denigma.kappa.messages.{Go, GoToFigure, GoToPaper, KappaMessage}
import org.denigma.kappa.notebook.parsers.{CommentLinksParser, ImageParser, PaperParser, VideoParser}
import org.denigma.kappa.notebook.views.MainTabs
import org.denigma.kappa.notebook.views.actions.Movements
import org.denigma.kappa.notebook.views.figures.{Figure, Image, Video}
import org.scalajs.dom.html.Anchor
import org.scalajs.dom.raw.MouseEvent
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scalatags.JsDom.all._
import org.denigma.binding.extensions._
import scala.concurrent.duration._
/**
  * Created by antonkulaga on 11/03/16.
  */
class CommentsWatcher(
                       val updates: Var[EditorUpdates],
                       val figures: Var[Map[String, Figure]],
                       val currentProjectName: Rx[String],
                       val input: Var[KappaMessage]
                     )  {

  updates.onChange(changeHandler) //subscription

  val commentsParser = new CommentLinksParser()
  def linkParser = commentsParser.link
  val paperParser = new PaperParser()
  val imageParser = new ImageParser()
  val videoParser = new VideoParser()

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
    val papers: List[((Int, String), String)] = lines.map{ case (num, line)=> (num, line) -> paperParser.paper.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }
    val links = lines.map{ case (num, line)=> (num, line) -> linkParser.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }

    val images: List[((Int, String), String)] = lines.map{ case (num, line)=> (num, line) -> imageParser.image.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }

    val videos: List[((Int, String), String)] = lines.map{ case (num, line)=> (num, line) -> videoParser.video.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }

    links.collectFirst{
      case ((n, line), result) if n == currentNum =>
        val marker = makeURIMarker(result)
        editor.setGutterMarker(n, "breakpoints", marker)
    }
    images.collectFirst{
      case ((n, line), result) if n == currentNum =>
        addImage(result)
        val marker = makeFigureMarker(result, "Image")
        editor.setGutterMarker(n, "breakpoints", marker)
    }
    videos.collectFirst{
      case ((n, line), result) if n == currentNum =>
        addVideo(result)
        val marker = makeFigureMarker(result, "Video")
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

  protected def cleanSrc(figure: String) = if(figure.contains("://") || figure.startsWith("/")) figure else currentProjectName.now + "/" + figure

  protected def addImage(figure: String) = {
    val src = cleanSrc(figure)
    if(!figures.now.contains(src)) {
      figures() = figures.now.updated(figure, Image(figure, src))
    }
  }

  protected def addVideo(figure: String) = {
    val src = cleanSrc(figure)
    if(!figures.now.contains(src)) {
      figures() = figures.now.updated(figure, Video(figure, src))
    }
  }

  protected def makeURIMarker(link: String): Anchor = {
    val tag = a(href := link, target := "blank",
      i(`class` := "label File Pdf Outline icon", id :="class_icon"+Math.random()),
      " "
    )
    tag.render
  }

  protected def makeFigureMarker(figure: String, icon: String) = {
    /*
    val tag = button(`class` := "ui icon tiny button", i(`class` := s"label File $icon Outline icon", onclick := {
      //println(s"mouse down on $num")
    }))
    */
    val tag = i(`class` := s"label pointed File $icon Outline icon", onclick := {
      //println(s"mouse down on $num")
    })
    val html = tag.render
    html.onclick = {
      event: MouseEvent =>
        input() = Movements.toFigure(figure)
    }
    html
  }

  protected def makePageMarker(paper: String) = {
    /*
    val tag = button(`class` := "ui icon tiny button", i(`class` := "label File Code Outline icon", onclick := {
      //println(s"mouse down on $num")
      }))
    */

    val tag = i(`class` := s"pointed label File Code Outline icon", onclick := {
      //println(s"mouse down on $num")
    })
    val html = tag.render
    html.onclick = {
      event: MouseEvent =>
        input() = Movements.toPaper(paper, 1)
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
