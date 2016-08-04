package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.extensions._
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.kappa.model.KappaModel._
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.parsers.{KappaParser, ParsedLine}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.concurrent.duration._


/**
  * Created by antonkulaga on 11/03/16.
  */
class KappaWatcher(cursor: Var[Option[(Editor, PositionLike)]], updates: Var[EditorUpdates])  {

  val parsed = Var(ParsedLine.empty)

  protected val kappaParser = new KappaParser

  protected val agentParser = kappaParser.agentDecl

  protected val ruleParser = kappaParser.rule

  protected val obsParser = kappaParser.observable

  protected val initParser = kappaParser.init

  val text: Rx[String] = cursor.map{
    case None => ""
    case Some((ed: Editor, lines)) =>
      val num = getStartNum(ed, lines.line)
      getEditorLine(ed, num)
 }


  protected def getStartNum(ed: Editor, line: Int): Int = {
    val doc = ed.getDoc()
    if(line > 1 && doc.getLine(line -1 ).trim.endsWith("\\")) getStartNum(ed, line -1) else line
  }

  protected def getEditorLine(ed: Editor, line: Int, acc: String = ""): String = {
    val doc = ed.getDoc()
    kappaParser.getKappaLine(doc.getLine)(line, doc.lineCount().toInt, acc)
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