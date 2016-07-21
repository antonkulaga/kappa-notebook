package org.denigma.kappa.notebook.views.editor

import fastparse.core.Parsed
import org.denigma.codemirror.{Editor, LineInfo, PositionLike}
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel._
import org.denigma.kappa.notebook.parsers.KappaParser
import org.denigma.kappa.notebook.views.visual._
import org.denigma.kappa.notebook.views.visual.rules.{AgentNode, KappaEdge}
import org.scalajs.dom.svg.SVG
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import org.denigma.threejs.extras.HtmlSprite
import rx.Rx.Dynamic
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.views.visual.rules.layouts._

import scalatags.JsDom

import scala.collection.immutable.SortedSet

/**
  * Created by antonkulaga on 11/03/16.
  */
class KappaWatcher(cursor: Var[Option[(Editor, PositionLike)]], updates: Var[EditorUpdates], s: SVG)  {

  val kappaParser = new KappaParser

  val agentParser = kappaParser.agentDecl

  val ruleParser = kappaParser.rule

  lazy val agentFontSize: Double = 24

  lazy val padding: Double= 10

  //lazy val painter: SpritePainter = new SpritePainter(agentFontSize, padding, s)

  val leftPattern: WatchPattern = new WatchPattern(s)

  val rightPattern: WatchPattern = new WatchPattern(s)

  val direction: Var[KappaModel.Direction] = Var(KappaModel.Left2Right)

  //protected val graivityForce = new Gravity(ForceLayoutParams.default2D.attractionMult, ForceLayoutParams.default2D.gravityMult, ForceLayoutParams.default2D.center)

  protected val borderForce = new BorderForce(ForceLayoutParams.default2D.repulsionMult, 10, 0.9, ForceLayoutParams.default2D.center)

  protected val forces: Vector[Force[AgentNode, KappaEdge]] = Vector(
    new Repulsion(ForceLayoutParams.default2D.repulsionMult),
    new Attraction(ForceLayoutParams.default2D.attractionMult),
    borderForce
  )


  val text: Rx[String] = cursor.map{
    case None => ""
    case Some((ed: Editor, lines)) => getKappaLine(ed, lines.line)
 }

  protected def getKappaLine(ed: Editor, line: Int, acc: String = ""): String = {
    val doc = ed.getDoc()
    val t = doc.getLine(line)
    val result = acc + t
    if(t.trim.endsWith("\\") && (line+ 1)< doc.lineCount()) getKappaLine(ed, line + 1, result) else result
  }

  text.onChange(t=>parseText(t))

  protected def parseText(line: String) = {
    if(line=="") {

    } else {
      agentParser.parse(line).onSuccess{
        case result =>
          val value = Pattern(List(result))
          leftPattern.refresh(value, forces)
          rightPattern.refresh(Pattern.empty, forces)

      }.onFailure{
        input=>
          ruleParser.parse(input).onSuccess{
            case rule =>
              direction() = rule.direction
              leftPattern.refresh(rule.left, forces)
              rightPattern.refresh(rule.right, forces)
              for {
                n <- leftPattern.nodes.now
              }
                if(rule.removed.contains(n.data)) n.markDeleted()
                else if(rule.added.contains(n.data)) n.markAdded()

              for {
                n <- rightPattern.nodes.now
              }
                if(rule.added.contains(n.data)) n.markAdded()
                else
                if(rule.modified.contains(n.data)) n.markChanged()
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

  def refresh(value: Pattern, forces: Vector[Force[AgentNode, KappaEdge]] ): Unit =  if(value != pattern.now) {
    layouts() = Vector.empty
    pattern() = value
    layouts() = Vector(new ForceLayout(nodes, edges, ForceLayoutParams.default2D.mode, forces))
  }

  def clean() = refresh(Pattern.empty, Vector.empty)

}