package org.denigma.kappa.notebook.views.comments

import fastparse.core.Parsed
import org.denigma.codemirror.Editor
import org.denigma.kappa.parsers.CommentLinksParser
import org.scalajs.dom.html.Anchor

import scalatags.JsDom.all._

class LinkWatcher(val commentsParser: CommentLinksParser) extends Watcher {

  def linkParser = commentsParser.link

  type Data = String

  override def parse(editor: Editor, lines: List[(Int, String)], currentNum: Int): Unit = {

    val links = lines.map{ case (num, line)=> (num, line) -> linkParser.parse(line) }.collect{
      case ((num, line), Parsed.Success(result, _))=> (num, line) -> result
    }

    links.collectFirst{
      case ((n, line), result) if n == currentNum =>
        val marker = makeMarker(result)
        editor.setGutterMarker(n, "breakpoints", marker)
    }
  }

  def makeMarker(link: String): Anchor = {
    val tag = a(href := link, target := "blank",
      i(`class` := "label at icon", id :="class_icon"+Math.random()),
      " "
    )
    tag.render
  }
}
