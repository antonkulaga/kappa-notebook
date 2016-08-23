package org.denigma.kappa.notebook.views.editor

import fastparse.all._
import org.denigma.binding.extensions._
import org.denigma.codemirror.Editor
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.actions.Movements
import org.denigma.kappa.notebook.parsers._
import org.denigma.kappa.notebook.views.figures.{Figure, Image, Video}
import org.denigma.kappa.notebook.views.settings.AnnotationMode
import org.scalajs.dom
import org.scalajs.dom.html.Anchor
import org.scalajs.dom.raw.MouseEvent
import rx._

import scalatags.JsDom.all._
/**
  * Created by antonkulaga on 11/03/16.
  */
class CommentsWatcher(
                       val updates: Var[EditorUpdates],
                       val figures: Var[Map[String, Figure]],
                       val currentProjectName: Rx[String],
                       val input: Var[KappaMessage],
                       val movements: Movements
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

    val paperSelections: List[((Int, String), PaperSelection)] = lines.map{ case (num, line)=> (num, line) -> paperParser.annotation.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=>
        //dom.console.log("PAPER LINE=="+line)
        (num, line) -> result
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
        /*
        * doc.addLineWidget(line: integer|LineHandle, node: Element, ?options: object) → LineWidget
    Adds a line widget, an element shown below a line, spanning the whole of the editor's width,
    and moving the lines below it downwards. line should be either an integer or a line handle,
    and node should be a DOM node, which will be displayed below the given line.
    options, when given, should be an object that configures the behavior of the widget.
    The following options are supported (all default to false):
        * */
    images.collectFirst{
      case ((n, line), result) if n == currentNum =>
        addImage(result)
        val marker = makeFigureMarker(result, "Image")
        editor.setGutterMarker(n, "breakpoints", marker)
        //val image = img(src := result).render
        //editor.getDoc().dyn.addLineWidget(n, "/files/"+image) //trying to add figure directly to the code
    }
    videos.collectFirst{
      case ((n, line), result) if n == currentNum =>
        addVideo(result)
        val marker = makeFigureMarker(result, "Video")
        editor.setGutterMarker(n, "breakpoints", marker)
    }

    paperSelections.collectFirst{
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
      i(`class` := "label at icon", id :="class_icon"+Math.random()),
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
        input() = movements.toFigure(figure)
    }
    html
  }

  protected def makePageMarker(paper: PaperSelection) = {
    /*
    val tag = button(`class` := "ui icon tiny button", i(`class` := "label File Code Outline icon", onclick := {
      //println(s"mouse down on $num")
      }))
    */
    //pointed label File Code Outline icon

    //label File Pdf Outline icon
    val tag = i(`class` := s"pointed label File Pdf Outline icon", onclick := {
      //println(s"mouse down on $num")
    })
    val html = tag.render
    html.onclick = {
      event: MouseEvent =>
          input() = movements.toPaper(paper)
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
