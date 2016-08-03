package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.extensions._
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel._
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.parsers.KappaParser
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.collection.immutable._
import org.denigma.binding.extensions._

import scala.List
import scala.concurrent.duration._

/**
  * Created by antonkulaga on 11/03/16.
  */
class KappaWatcher(cursor: Var[Option[(Editor, PositionLike)]], updates: Var[EditorUpdates])  {

  val kappaParser = new KappaParser

  val agentParser = kappaParser.agentDecl

  val ruleParser = kappaParser.rule

  val obsParser = kappaParser.observable

  val initParser = kappaParser.init

  val leftPattern: Var[Pattern] = Var(Pattern.empty)

  val rightPattern: Var[Pattern] = Var(Pattern.empty)

  val direction: Var[KappaModel.Direction] = Var(KappaModel.Left2Right)

  //protected val graivityForce = new Gravity(ForceLayoutParams.default2D.attractionMult, ForceLayoutParams.default2D.gravityMult, ForceLayoutParams.default2D.center)

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

  protected def getKappaLine(getLine: Double => String)(line: Int, count: Int, acc: String = ""): String = {
    val t: String = getLine(line).trim
    if(t.endsWith("\\") && (line+ 1)< count) {
      val newLine =" " + (t.indexOf("#") match {
        case -1 => t.dropRight(1)
        case index =>
          val withoutComment: String = t.dropRight(t.length - index)
          withoutComment
      })
      getKappaLine(getLine)(line + 1, count, acc+ newLine)
    } else (acc+ " " + t).trim
  }

  protected def getEditorLine(ed: Editor, line: Int, acc: String = ""): String = {
    val doc = ed.getDoc()
    getKappaLine(doc.getLine)(line, doc.lineCount().toInt, acc)
  }

  text.afterLastChange(300 millis)(t=>parseText(t))

  val isRule = Var(true)

  protected def parseText(line: String) =
    if(line=="") {

    } else {
      agentParser.parse(line).onSuccess{
        result =>
          val value = Pattern(List(result))
          isRule() = false
          leftPattern() = value
          rightPattern() = Pattern.empty
      }.onFailure{
        input=>
          ruleParser.parse(input).onSuccess{
            rule =>
              isRule() = true
              direction() = rule.direction
              leftPattern() = rule.left
              rightPattern() = rule.right
          }.onFailure{
            input2=>
              obsParser.parse(input2).onSuccess{
                result =>
                  isRule() = false
                  leftPattern() = result.pattern
                  rightPattern() = Pattern.empty
              }.onFailure{
                input3=>
                  initParser.parse(input3).onSuccess{
                    result =>
                      isRule() = false
                      leftPattern() = result.pattern
                      rightPattern() = Pattern.empty
                  }
          }
      }
    }
  }

  val sameAgents: Rx[List[(Agent, Agent)]] = Rx{
    val lp = leftPattern()
    if(isRule()) {
      val rp = rightPattern()
      lp.sameAgents(rp)
    }
    else lp.agents.zip(lp.agents)
  }

  val unchangedAgents: Rx[Set[Agent]] = Rx{
      sameAgents().collect{ case (one, two) if one ==two => one}.toSet
  }

  val updatedAgents: Rx[Set[(Agent, Agent)]] = Rx{
    sameAgents().collect{ case (one, two) if one != two => one -> two}.toSet
  }

  val modifiedAgents: Rx[(List[Agent], List[Agent])] = sameAgents.map{
      case ags =>
        val unzip: (List[Agent], List[Agent]) = ags.filterNot{ case (one, two)=> one==two}.unzip{case (a, b)=> (a, b)}
        unzip
  }
  val leftModified = modifiedAgents.map(_._1.toSet)
  val rightModified = modifiedAgents.map(_._2.toSet)

  val removedAgents: Rx[Set[Agent]] = Rx{
    val l = leftModified()
    val u = unchangedAgents()
    leftPattern().agents.filterNot(p => l.contains(p) || u.contains(p) ).toSet
  }

  val addedAgents: Rx[Set[Agent]] = Rx{
    val r = rightModified()
    val u = unchangedAgents()
    rightPattern().agents.filterNot(p => r.contains(p) || u.contains(p) ).toSet
  }

  /*
  val leftUnchanged = Rx{
    println(s"IS RULE = ${isRule.now}")
    if(isRule()) {
      unchangedAgents()
    } else leftPattern().agents.toSet
  }
  val rightUnchanged = Rx{
    println(s"IS RULE2 = ${isRule.now}")
    if(isRule()) unchangedAgents() else Set.empty[Agent]
  }


  val removed: Rx[Set[Agent]] = Rx{
    val unchanged = leftUnchanged()
    val mod = leftModified()
    val same = unchanged ++ mod
    val lp = leftPattern()
    println("unchanged")
    println(unchanged)
    println("mod")
    println(mod)
    println("removed")
    println(lp.agents.filterNot(same.contains).toSet)
    lp.agents.filterNot(same.contains).toSet
  }

  val added: Rx[Set[Agent]] = Rx{
    val unchanged = unchangedAgents()
    val mod = rightModified()
    val same = unchanged ++ mod
    val rp = rightPattern()
    rp.agents.filterNot(same.contains).toSet
  }
  */
}