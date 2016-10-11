package org.denigma.kappa.notebook.views.comments

import fastparse.all._
import org.denigma.binding.extensions._
import org.denigma.codemirror.Editor
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.parsers._
import org.denigma.kappa.notebook.views.editor.EditorUpdates
import org.denigma.kappa.parsers.CommentLinksParser
import rx._

class CommentsWatcher(
                       val updates: Var[EditorUpdates],
                       val input: Var[KappaMessage]
                     )  {

  updates.onChange(changeHandler) //subscription

  lazy val commentsParser = new CommentLinksParser
  lazy val filesParser = new FilesParser()

  val figuresWatcher = new FiguresWatcher(filesParser, input)
  val paperWathcer = new PaperWatcher(new PaperParser, input)
  val linkWatcher = new LinkWatcher(commentsParser)
  val sourceWatcher = new SourceWatcher(filesParser, input)

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

  protected def semanticSearch(editor: Editor, lines: List[(Int, String)], currentNum: Int): Unit = {
    paperWathcer.parse(editor, lines, currentNum)
    linkWatcher.parse(editor, lines, currentNum)
    sourceWatcher.parse(editor, lines, currentNum)
    figuresWatcher.parse(editor, lines, currentNum)
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
