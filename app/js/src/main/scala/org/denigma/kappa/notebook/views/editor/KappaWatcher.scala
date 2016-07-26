package org.denigma.kappa.notebook.views.editor

import fastparse.core.Parsed
import org.denigma.codemirror.{Doc, Editor, LineInfo, PositionLike}
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel._
import org.denigma.kappa.notebook.parsers.KappaParser
import org.denigma.kappa.notebook.views.visual._
import org.scalajs.dom.svg.SVG
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import org.denigma.threejs.extras.HtmlSprite
import rx.Rx.Dynamic
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.graph.layouts._

import scalatags.JsDom
import scala.collection.immutable.SortedSet

/**
  * Created by antonkulaga on 11/03/16.
  */
class KappaWatcher(cursor: Var[Option[(Editor, PositionLike)]], updates: Var[EditorUpdates])  {

  val kappaParser = new KappaParser

  val agentParser = kappaParser.agentDecl

  val ruleParser = kappaParser.rule

  lazy val agentFontSize: Double = 24

  lazy val padding: Double= 10

  //lazy val painter: SpritePainter = new SpritePainter(agentFontSize, padding, s)

  //val leftPattern: WatchPattern = new WatchPattern(s)

  //val rightPattern: WatchPattern = new WatchPattern(s)

  val leftPattern: Var[Pattern] = Var(Pattern.empty)

  val rightPattern: Var[Pattern] = Var(Pattern.empty)


  val direction: Var[KappaModel.Direction] = Var(KappaModel.Left2Right)

  //protected val graivityForce = new Gravity(ForceLayoutParams.default2D.attractionMult, ForceLayoutParams.default2D.gravityMult, ForceLayoutParams.default2D.center)

  val text: Rx[String] = cursor.map{
    case None => ""
    case Some((ed: Editor, lines)) => getEditorLine(ed, lines.line)
 }

  protected def getKappaLine(getLine: Double => String)(line: Int, count: Int, acc: String = ""): String = {
    val t = getLine(line).trim
    if(t.endsWith("\\") && (line+ 1)< count) {
      val newLine =" " + (t.indexOf("#") match {
        case -1 => t.dropRight(1)
        case index => t.dropRight(t.length - index)
      })
      getKappaLine(getLine)(line + 1, count, acc+ newLine)
    } else (acc+ " " + t).trim
  }

  protected def getEditorLine(ed: Editor, line: Int, acc: String = ""): String = {
    val doc = ed.getDoc()
    getKappaLine(doc.getLine)(line, doc.lineCount().toInt, acc)
  }

  text.onChange(t=>parseText(t))

  protected def parseText(line: String) = {
    if(line=="") {

    } else {
      agentParser.parse(line).onSuccess{
        case result =>
          val value = Pattern(List(result))
          leftPattern() = value
          rightPattern() = Pattern.empty
          //leftPattern.refresh(value, forces)
          //rightPattern.refresh(Pattern.empty, forces)

      }.onFailure{
        input=>
          ruleParser.parse(input).onSuccess{
            case rule =>
              direction() = rule.direction
              leftPattern() = rule.left
              rightPattern() = rule.right

              //leftPattern.refresh(rule.left, forces)
              //rightPattern.refresh(rule.right, forces)
              /*
              for {
                n <- leftPattern.nodes.now
              }
                if(rule.removed.contains(n.agent)) n.markDeleted() else if(rule.added.contains(n.agent)) n.markAdded()

              for {
                n <- rightPattern.nodes.now
              }
                if(rule.added.contains(n.agent)) n.markAdded() else if(rule.modified.contains(n.agent)) n.markChanged()
              */
              /*
              val (chLeft, chRight) = rule
              for{
                r <- rule.removed
                n <- leftPattern.nodes.now
                if r == n.data
              } {
                if(chLeft.contains(r)) n.markChanged() else n.markDeleted()
              }
              for{
                a <- rule.added
                n <- rightPattern.nodes.now
                if n.data == a
              } {
                if(chRight.contains(a)) n.markChanged() else n.markDeleted()
              }
              */
          }
      }
    }
  }

  protected def changeHandler(editor: Editor, lines: Seq[(Int, String)]) =
  {
    for {
      (num, line) <- lines
    } {

      //searchForAgents(editor, line , num)
    }
  }


}
/*
class WatchPattern(s: SVG) {

  val pattern = Var(Pattern.empty)

  val agents = pattern.map(p=>p.agents)

  val links: Rx[SortedSet[Link]] = pattern.map(p => SortedSet(p.links.values.toSeq:_*))

  import org.denigma.kappa.notebook.extensions._

  protected def agent2node(agent: KappaModel.Agent) = { new AgentNode(agent, s)  }

  val layouts: Var[Vector[GraphLayout]] = Var{Vector.empty}


  val agentMap: Rx[Map[Agent, AgentNode]] = agents.map{
    case ags => ags.map(a => a -> agent2node(a)).toMap
  }

  val nodes: Rx[Vector[AgentNode]] = agentMap.map(mp => mp.values.toVector)//agents.toSyncVector(agent2node)((a, n)=> n.data == a)

  /*
  val edges: Rx[Vector[KappaEdge]] = Rx{
    val mp = agentMap()
    val ls = links()
    val result = (for{
      link <- ls
      from <- mp.get(link.fromAgent)
      to <- mp.get(link.toAgent)
    }
      yield {
        //val sprite = painter.drawLink(link)
        //val sp = new HtmlSprite(sprite.render)
        new KappaEdge(link, from, to, s = this.s)
      }).toVector
    //println("EDGES NUMBER = "+result.length)
    result
  }
  */

  def refresh(value: Pattern, forces: Vector[Force[AgentNode, KappaEdge]] ): Unit =  if(value != pattern.now) {
    layouts() = Vector.empty
    pattern() = value
    //layouts() = Vector(new RulesForceLayout(nodes, edges, ForceLayoutParams.default2D.mode, forces))
  }

  def clean() = refresh(Pattern.empty, Vector.empty)

}
*/