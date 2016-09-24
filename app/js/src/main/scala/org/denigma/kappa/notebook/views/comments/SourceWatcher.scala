package org.denigma.kappa.notebook.views.comments

import fastparse.core.Parsed
import org.denigma.codemirror.Editor
import org.denigma.kappa.messages.{Animate, Go, KappaMessage}
import org.denigma.kappa.notebook.parsers.{AST, FilesParser}
import org.scalajs.dom.MouseEvent
import rx.Var

import scalatags.JsDom.all._

class SourceWatcher(val filesParser: FilesParser, input: Var[KappaMessage]) extends Watcher{


  override type Data = (AST.IRI, Option[Int])

  override def parse(editor: Editor, lines: List[(Int, String)], currentNum: Int): Unit = {

    val sources =  lines.map{ case (num, line)=> (num, line) -> filesParser.source.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _)) => (num, line) -> result
    }

    val nums: List[((Int, String), Int)] = lines.map{ case (num, line) => (num, line) -> filesParser.onLine.parse(line) }.collect {
      case ((num, line), Parsed.Success((_, result), _)) => (num, line) -> result
    }

    sources.collectFirst{
      case ((n, line), (_, file, num)) if n == currentNum =>
        //dom.console.log("SOURCE PARSE = "+result)
        val marker = makeMarker((file, num))
        editor.setGutterMarker(n, "breakpoints", marker)
    }

    nums.collectFirst{
      case ((n, line), result) if n == currentNum =>
        //dom.console.log("SOURCE NUM PARSE = "+result)
        val marker = makeLineMarker(result)
        editor.setGutterMarker(n, "breakpoints", marker)
    }
  }

  protected def makeLineMarker(n: Int) = {

    val tag = i(`class` := s"pointed label File Code Outline icon", onclick := {
      //println(s"mouse down on $num")
    })
    val html = tag.render
    html.onclick = {
          event: MouseEvent =>
            input() = Animate(Go.ToSource(AST.IRI(""), n, n), true)
    }
    html
  }

  protected def makeMarker(data: Data) = {

    val tag = i(`class` := s"pointed label File Code Outline icon", onclick := {
      //println(s"mouse down on $num")
    })
    val html = tag.render
    data match {
      case (iri, Some(num)) =>
        html.onclick = {
          event: MouseEvent =>
            input() = Animate(Go.ToSource(iri, num, num), true)
        }
      case (iri, None) =>
        html.onclick = {
          event: MouseEvent =>
            println("IRI IS "+iri)
            input() = Animate(Go.ToSource(iri, 0, 0), true)
        }
    }

    html
  }
}
