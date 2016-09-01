package org.denigma.kappa.notebook.views.comments

import fastparse.all._
import org.denigma.codemirror.Editor
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.actions.Movements
import org.denigma.kappa.notebook.parsers.{AST, FilesParser}
import org.denigma.kappa.notebook.views.figures.{Figure, Image, Video}
import org.scalajs.dom
import org.scalajs.dom.html.Anchor
import org.scalajs.dom.raw.MouseEvent
import rx.Var

import scalatags.JsDom.all._

class FiguresWatcher(val filesParser: FilesParser, val input: Var[KappaMessage], val movements: Movements) extends Watcher {

  override type Data = AST.IRI

  override def parse(editor: Editor, lines: List[(Int, String)], currentNum: Int): Unit = {

    val images = lines.map{ case (num, line)=> (num, line) -> filesParser.image.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> Image(result.path, result.path)
    }

    images.collectFirst{
      case ((n, line), result) if n == currentNum =>
        //dom.console.log("IMAGE PARSE = "+result)
        val marker = makeFigureMarker(result, "Image")
        editor.setGutterMarker(n, "breakpoints", marker)
      //val image = img(src := result).render
      //editor.getDoc().dyn.addLineWidget(n, "/files/"+image) //trying to add figure directly to the code
    }


    val videos = lines.map{ case (num, line)=> (num, line) -> filesParser.video.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> Video(result.path, result.path)
    }

    videos.collectFirst{
      case ((n, line), result) if n == currentNum =>
       // dom.console.log("VIDEO PARSE = "+result)
        val marker = makeFigureMarker(result, "Video")
        editor.setGutterMarker(n, "breakpoints", marker)
    }

    /**
      * doc.addLineWidget(line: integer|LineHandle, node: Element, ?options: object) → LineWidget
    Adds a line widget, an element shown below a line, spanning the whole of the editor's width,
    and moving the lines below it downwards. line should be either an integer or a line handle,
    and node should be a DOM node, which will be displayed below the given line.
    options, when given, should be an object that configures the behavior of the widget.
    The following options are supported (all default to false):
      * */
  }

  protected def makeFigureMarker(figure: Figure, icon: String) = {

    val tag = i(`class` := s"label pointed File $icon Outline icon", onclick := {
      //println(s"mouse down on $num")
    })
    val html = tag.render
    html.onclick = {
      event: MouseEvent =>
        dom.console.log("MOVES TO FIGURE:")
        pprint.pprintln(figure)
        input() = movements.toFigure(figure)
    }
    html
  }
}
