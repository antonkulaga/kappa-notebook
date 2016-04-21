package org.denigma.kappa.notebook.views.editor

import fastparse.core.Parsed
import org.denigma.codemirror.{Editor, LineInfo, PositionLike}
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

import scalatags.JsDom
/*
object Tester {

  val agents =  scala.collection.immutable.SortedSet(
    KappaModel.Agent("LacI_RNA"),
    KappaModel.Agent("LacI", List( Side("left"), Side("right"), Side("dna") )),
    KappaModel.Agent("LacI_unf"),
    KappaModel.Agent("pLacAra", List( Side("araC"), Side("lacI1"), Side("lacI2"), Side("down") )),
    KappaModel.Agent("LacI_DNA", List(Side("up"))),
    KappaModel.Agent("AraC_DNA", List(Side("up"))),
    KappaModel.Agent("AraC_RNA"),
    KappaModel.Agent("AraC", List( Side("ara"), Side("dna") )),
    KappaModel.Agent("AraC_unf")
  )
}
*/

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

  lazy val painter: AgentPainter = new AgentPainter(agentFontSize, padding, s)

  val leftPattern = new WatchPattern(painter)

  val rightPattern = new WatchPattern(painter)

  val direction: Var[KappaModel.Direction] = Var(KappaModel.Left2Right)

  //protected val graivityForce = new Gravity(ForceLayoutParams.default2D.attractionMult, ForceLayoutParams.default2D.gravityMult, ForceLayoutParams.default2D.center)

  protected val borderForce = new BorderForce(ForceLayoutParams.default2D.repulsionMult , 10, 0.9, ForceLayoutParams.default2D.center)

  protected val forces: Vector[Force[KappaNode, KappaEdge]] = Vector(
    new Repulsion(ForceLayoutParams.default2D.repulsionMult),
    new Attraction(ForceLayoutParams.default2D.attractionMult),
    borderForce
  )


  val text: Rx[String] = cursor.map{
    case None => ""
    case Some((ed: Editor, lines)) =>
      val t = ed.getDoc().getLine(lines.line)
      t
 }
  text.onChange(parseText)


  protected def parseText(line: String) = {
    if(line=="") {

    } else {
      agentParser.parse(line).onSuccess{
        case result =>
          val value = Pattern(List(result))
        leftPattern.refresh(value, forces)

      }.onFailure{
        input=>
          ruleParser.parse(input).onSuccess{
            case result =>
              leftPattern.refresh(result.left, forces)
              rightPattern.refresh(result.right, forces)
              direction() = result.direction
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
  /*

  val changeOptions: Dynamic[Option[(Editor, Seq[(Int, String)])]] = updates.map(upd=>upd.changedLinesOpt)
  changeOptions.foreach{
    case Some((editor, lines))=> changeHandler(editor, lines)
    case other => //nothing
  }
  */


}
class WatchPattern(painter: AgentPainter) {

  val pattern = Var(Pattern.empty)

  val agents = pattern.map(p=>p.agents)

  val links: Rx[SortedSet[Link]] = pattern.map(p => SortedSet(p.links.values.toSeq:_*))

  import org.denigma.kappa.notebook.extensions._

  protected def agent2node(agent: KappaModel.Agent) = {
    val sprite = painter.drawAgent(agent)
    val sp = new HtmlSprite(sprite.render)
    new KappaNode(agent, sp)
  }



  val layouts: Var[Vector[GraphLayout]] = Var{Vector.empty}


  val agentMap: Rx[Map[Agent, KappaNode]] = agents.map{
    case ags => ags.map(a => a -> agent2node(a)).toMap
  }

  val nodes: Rx[Vector[KappaNode]] = agentMap.map(mp => mp.values.toVector)//agents.toSyncVector(agent2node)((a, n)=> n.data == a)

  val edges = Rx{
    val mp = agentMap()
    val ls = links()
    (for{
      link <- ls
      from <- mp.get(link.fromAgent)
      to <- mp.get(link.toAgent)
    }
      yield {
        val sprite = painter.drawLink(link)
        val sp = new HtmlSprite(sprite.render)
        new KappaEdge(from, to, sp)
      }).toVector
  }

  def refresh(value: Pattern, forces: Vector[Force[KappaNode, KappaEdge]] ): Unit =  if(value != pattern.now) {
    layouts() = Vector.empty
    pattern() = value
    layouts() = Vector(new ForceLayout(nodes, edges, ForceLayoutParams.default2D.mode, forces))
  }

  def clean() = refresh(Pattern.empty, Vector.empty)

}