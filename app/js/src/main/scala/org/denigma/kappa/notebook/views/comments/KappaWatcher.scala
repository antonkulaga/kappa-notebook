package org.denigma.kappa.notebook.views.comments

import org.denigma.binding.extensions._
import org.denigma.codemirror.Editor
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.views.editor.{EditorUpdates, EmptyCursor, KappaCursor, KappaEditorCursor}
import org.denigma.kappa.parsers.{KappaParser, ParsedLine}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.concurrent.duration._
import scalatags.JsDom.all._

/**
  * Used by vizualizations - extracts kappa rules/agents/obs/etc. from chosen and neibouring lines
  * @param cursor
  * @param updates
  */
class KappaWatcher(cursor: Var[KappaCursor], updates: Var[EditorUpdates])  {

  val parsed = Var(ParsedLine.empty)

  protected val kappaParser = new KappaParser

  protected val agentParser = kappaParser.agentDecl

  protected val ruleParser = kappaParser.rule

  protected val obsParser = kappaParser.observable

  protected val initParser = kappaParser.init

  lazy val cursorChanges = cursor.zip
  cursorChanges.onChange{
    case (KappaEditorCursor(file, editor, lineNum, ch), _) =>
      editor.getGutterElement() match {
        case null =>
        case e if e.classList.contains("viz") =>
          //TODO: finish
        case other =>
      }
    case _=>
  }

  val text: Rx[String] = cursor.map{
    case EmptyCursor => ""
    case KappaEditorCursor(file, editor, lineNum, ch) =>
      val num = getStartNum(editor, lineNum)
      getEditorLine(editor, num)
 }


  protected def getStartNum(ed: Editor, line: Int): Int = {
    val doc = ed.getDoc()
    if(line > 1 && doc.getLine(line -1 ).trim.endsWith("\\")) getStartNum(ed, line -1) else line
  }

  protected def getEditorLine(ed: Editor, line: Int, acc: String = ""): String = {
    val doc = ed.getDoc()
    def extractLine(num: Double): String ={
      val l = doc.getLine(num)
      val str = if(l.endsWith("\n")) l.dropRight(1) else l
      val result = str.indexOf('#') match {
        case -1 =>l
        case ind if str.endsWith("\\") =>
          l.substring(0, ind).trim match {
            case v => v+ "\\"
          }
        case ind => l.substring(0, ind)
      }
      result
    }
    kappaParser.getKappaLine(extractLine)(line, doc.lineCount().toInt, acc)
  }

  text.afterLastChange(400 millis)(t=>parseText(t))

  protected def parseText(line: String) =
    if(line=="") {

    } else {
      agentParser.parse(line).onSuccess{
        result => parsed() = ParsedLine(line, result)
      }.onFailure{
        input=>
          ruleParser.parse(input).onSuccess{
              result => parsed() = ParsedLine(line, result)
          }.onFailure{
            input2=>
              obsParser.parse(input2).onSuccess{
                  result => parsed() = ParsedLine(line, result)

              }.onFailure{
                input3=>
                  initParser.parse(input3).onSuccess{
                      result => parsed() = ParsedLine(line, result)
                  }.onFailure{
                    _ => parsed() = ParsedLine.empty
                  }
          }
      }
    }
  }

}