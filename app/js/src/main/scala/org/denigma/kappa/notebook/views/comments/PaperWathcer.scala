package org.denigma.kappa.notebook.views.comments


import fastparse.all._
import org.denigma.codemirror.Editor
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.actions.Movements
import org.denigma.kappa.notebook.parsers.{PaperParser, PaperSelection}
import org.scalajs.dom.html.Anchor
import org.scalajs.dom.raw.MouseEvent
import rx.Var

import scalatags.JsDom.all._

class PaperWathcer(paperParser: PaperParser, input: Var[KappaMessage], movements: Movements) extends Watcher {
  override type Data = PaperSelection

  override def parse(editor: Editor, lines: List[(Int, String)], currentNum: Int): Unit = {

    val paperSelections: List[((Int, String), PaperSelection)] = lines.map{ case (num, line)=> (num, line) -> paperParser.annotation.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=>
        //dom.console.log("PAPER LINE=="+line)
        (num, line) -> result
    }
    paperSelections.collectFirst{
      case ((n, line), result) if n == currentNum =>
        val marker = makeMarker(result)
        editor.setGutterMarker(n, "breakpoints", marker)
    }
  }




  protected def makeMarker(paper: PaperSelection) = {
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

}
