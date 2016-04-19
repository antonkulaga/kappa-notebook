package org.denigma.kappa.notebook.views.editor

import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.Side
import org.denigma.kappa.notebook.parsers.KappaParser
import org.denigma.kappa.notebook.views.visual._
import org.scalajs.dom.svg.SVG
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe


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

  val ruleParser = kappaParser.rulePart

  val agents: Var[SortedSet[KappaModel.Agent]] = Var(Tester.agents)

  import org.denigma.kappa.notebook.extensions._

  val nodes = agents.toSyncVector(agent2node)((a, n)=> n.data == a)

  val edges = Var(Vector.empty[KappaEdge])

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

  val layouts = Rx{
    //if(agents.)
    Vector(forceLayout)
  }
  //updates.foreach(changeHandler) //subscription
  /*

  val changeOptions: Dynamic[Option[(Editor, Seq[(Int, String)])]] = updates.map(upd=>upd.changedLinesOpt)
  changeOptions.foreach{
    case Some((editor, lines))=> changeHandler(editor, lines)
    case other => //nothing
  }


  protected def searchForAgents(editor: Editor, line: String, num: Int) = {
    agentParser.parse(line) match {
      case Parsed.Success(result, index) =>
        println("found agent: " + result)
        agents() = SortedSet(result)

      case Parsed.Failure(parser, index, extra) =>
    }
  }

  protected def changeHandler(editor: Editor, lines: Seq[(Int, String)]) =
  {
    for {
      (num, line) <- lines
    } {
      searchForAgents(editor, line , num)
    }
  }
  */
}
