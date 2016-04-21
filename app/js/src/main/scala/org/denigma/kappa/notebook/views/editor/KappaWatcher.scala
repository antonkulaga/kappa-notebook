package org.denigma.kappa.notebook.views.editor

import fastparse.core.Parsed
import org.denigma.codemirror.{Editor, LineInfo, PositionLike}
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{Agent, Link, Pattern, Side}
import org.denigma.kappa.notebook.parsers.KappaParser
import org.denigma.kappa.notebook.views.visual._
import org.scalajs.dom.svg.SVG
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import org.denigma.threejs.extras.HtmlSprite
import rx.Rx.Dynamic
import org.denigma.kappa.notebook.extensions._

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

import scala.collection.immutable.SortedSet

/**
  * Created by antonkulaga on 11/03/16.
  */
class KappaWatcher(cursor: Var[Option[(Editor, PositionLike)]], updates: Var[EditorUpdates], s: SVG)  {

  val kappaParser = new KappaParser

  val agentParser = kappaParser.agentDecl

  val ruleParser = kappaParser.rule

  val agents: Var[SortedSet[KappaModel.Agent]] = Var(Tester.agents)

  val pattern: Rx[Pattern] = agents.map(ags=>Pattern(ags.toList))

  val agentMap: Rx[Map[Agent, KappaNode]] = agents.map{
    case ags => ags.map(a => a -> agent2node(a)).toMap
  }

  val links: Rx[SortedSet[Link]] = pattern.map(p => SortedSet(p.links.values.toSeq:_*))

  import org.denigma.kappa.notebook.extensions._

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

  lazy val agentFontSize: Double = 24

  lazy val padding: Double= 10

  lazy val painter = new AgentPainter(agentFontSize, padding, s)

  protected def agent2node(agent: KappaModel.Agent) = {
    val sprite = painter.drawAgent(agent)
    val sp = new HtmlSprite(sprite.render)
    new KappaNode(agent, sp)
  }

  val graivityForce = new Gravity(ForceLayoutParams.default2D.attractionMult, ForceLayoutParams.default2D.gravityMult, ForceLayoutParams.default2D.center)

  val borderForce = new BorderForce(ForceLayoutParams.default2D.repulsionMult , 100, 2, ForceLayoutParams.default2D.center)


  val forces: Vector[Force[KappaNode, KappaEdge]] = Vector(
    new Repulsion(ForceLayoutParams.default2D.repulsionMult),
    new Attraction(ForceLayoutParams.default2D.attractionMult),
    borderForce
  )

  protected val forceLayout = new ForceLayout(nodes, edges, ForceLayoutParams.default2D.mode, forces)

  val layouts: Var[Vector[GraphLayout]] = Var{
    //if(agents.)
    Vector(forceLayout)
  }

  val text: Rx[String] = cursor.map{
    case None => ""
    case Some((ed: Editor, lines)) =>
      val t = ed.getDoc().getLine(lines.line)
      //println("lines is == "+t)
      t
 }

  text.onChange(parseText)

  protected def refreshAgents(value: SortedSet[Agent], ls: Vector[GraphLayout] = Vector(forceLayout)) = if(value == agents.now) {
    println("SHOULD BE EQUAL:\n" + agents.now+ "\n AND \n"+value)

  }
  else {
    layouts() = Vector.empty
    agents() = value
    layouts() = ls
  }

  protected def parseText(line: String) = {
    if(line=="") {

    } else {
      agentParser.parse(line).onSuccess{
        case result =>
          val value = SortedSet(result)
          refreshAgents(value)

      }.onFailure{
        input=>
          ruleParser.parse(input).onSuccess{
            case result =>
              val value = SortedSet(result.left.agents:_*)
              refreshAgents(value)
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
