package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.kappa.model.KappaModel.Agent
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.layouts._
import org.scalajs.dom
import org.scalajs.dom.raw.{ClientRect, Element}
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._
import scala.collection.immutable._

object RulesVisualSettings
{
  val DEFAULT_AGENT_FONT = 34
  val DEFAULT_SITE_FONT = 20
  val DEFAULT_STATE_FONT = 14
}

case class RulesVisualSettings(
                           canvas: SVG,
                           agent: KappaNodeVisualSettings = KappaNodeVisualSettings(RulesVisualSettings.DEFAULT_AGENT_FONT, 5),
                           sites: KappaNodeVisualSettings  = KappaNodeVisualSettings(RulesVisualSettings.DEFAULT_SITE_FONT, 3),
                           state: KappaNodeVisualSettings = KappaNodeVisualSettings(RulesVisualSettings.DEFAULT_STATE_FONT, 2),
                           link: KappaEdgeVisualSettings = KappaEdgeVisualSettings(14, 4, LineParams(lineColor = Colors.blue)),
                           otherArrows: LineParams = LineParams(lineColor = 0x000000)
                         )

class RulesGraphView(val elem: Element,
                     val unchanged: Rx[Set[Agent]],
                     val removed: Rx[Set[Agent]],
                     val added: Rx[Set[Agent]],
                     val updated: Rx[Set[Agent]],
                     val containerName: String,
                     val visualSettings: RulesVisualSettings
               ) extends BindableView {

  lazy val size: (Double, Double) = elem.getBoundingClientRect() match {
    case rect: ClientRect if rect.width < 100 || rect.height < 100 => (400.0, 400.0)
    case rect: ClientRect => (rect.width, rect.height)
  }

  val width: Var[Double] = Var(size._1)

  val height: Var[Double] = Var(size._2)

  val container = sq.byId(containerName).get

  type Node = KappaNode

  type Edge = KappaEdge

  val agents: Rx[Set[Agent]] = Rx{
    unchanged() ++ removed() ++ added() ++ updated()
  }


  implicit protected def createAgentNodeView(agent: AgentNode): KappaAgentView = {
    new KappaAgentView(agent.agent.name, visualSettings.agent.font, visualSettings.agent.padding, visualSettings.canvas)
  }

  implicit protected def createSiteNodeView(site: SiteNode): KappaSiteView = {
    new KappaSiteView(site.site.name, visualSettings.sites.font, visualSettings.sites.padding, visualSettings.canvas)
  }

  implicit protected def createStateNodeView(state: StateNode): KappaStateView = {
    new KappaStateView(state.state.name, visualSettings.state.font, visualSettings.state.padding, visualSettings.canvas)
  }

  implicit protected def createLinkView(edge: KappaLinkEdge): KappaLinkView = {
    new KappaLinkView(edge.link.label, visualSettings.link.font, visualSettings.link.padding, visualSettings.canvas)
  }

  //protected val gravityForce = new Gravity[Node, Edge](ForceLayoutParams.default2D.attractionMult / 4, ForceLayoutParams.default2D.gravityMult, ForceLayoutParams.default2D.center)
  protected val repulsionForce = new Repulsion[Node, Edge](ForceLayoutParams.default2D.repulsionMult)(compareRepulsion)
  protected val springForce = new SpringForce[Node, Edge](ForceLayoutParams.default2D.springMult)(computeSpring)
  protected val borderForce = new BorderForce[Node, Edge](ForceLayoutParams.default2D.repulsionMult / 5, 10, 0.9, ForceLayoutParams.default2D.center)

  protected def compareRepulsion(node1: Node, node2: Node): (Double, Double) = (massByNode(node1), massByNode(node2))

  val minSpring = 95

  def massByNode(node: KappaNode): Double = node match {
    case n: AgentNode => 1.5
    case s: SiteNode => 1.0
    case st: StateNode => 0.8
  }

  protected def computeSpring(edge: Edge): SpringParams = (edge.from, edge.to) match {
    case (from: SiteNode, to: AgentNode) => SpringParams(minSpring, 2, massByNode(from), massByNode(to))
    case (from: AgentNode, to: AgentNode) => SpringParams(minSpring, 2, massByNode(from), massByNode(to))
    case (from: KappaNode, to: KappaNode) => SpringParams(minSpring, 1, massByNode(from), massByNode(to))
  }

  protected val forces: Vector[Force[ Node, Edge]] = Vector(
    repulsionForce,
    springForce
    //gravityForce
    //,borderForce
  )

  val agentNodes: Var[Set[AgentNode]] = Var(Set.empty[AgentNode])

  val siteNodes: Rx[Set[SiteNode]] = agentNodes.map(ags => ags.flatMap(ag=>ag.children))

  val stateNodes: Rx[Set[StateNode]] = siteNodes.map(sn => sn.flatMap(s =>s.children))

  val links: Rx[Map[String, List[SiteNode]]] = siteNodes.map{
    case sNodes =>
      sNodes.toList
        .flatMap(node =>  node.site.links.map(l=> (node , l)))
        .groupBy{case (site, link)=> link}
        .mapValues(lst => lst.map{
          case (key, value) => key })
  }


  val allNodes: Rx[Set[Node]] = Rx{
    agentNodes() ++ siteNodes() ++ stateNodes()//++ links()
  }

  val nodes: Rx[Vector[Node]] = allNodes.map(n=>n.toVector)//Var(Vector.empty[Node])

  val edges = Var(Vector.empty[Edge])

  val layouts = Var(Vector(new RulesForceLayout(nodes, edges, ForceLayoutParams.default2D.mode, forces)))

  val iterationsPerFrame = Var(5)
  val firstFrameIterations = Var(50)

  val viz = new Visualizer(container,
    width,
    height,
    layouts,
    900.0,
    iterationsPerFrame,
    firstFrameIterations
  )
  protected def onAgentsUpdate(agentsRemoved: Set[Agent], agentsAdded: Set[Agent]) = {
    firstFrameIterations() = 50
    val addedNodes = agentsAdded.map{
      case a =>
        val n = new AgentNode(a)
        if(removed.now.contains(a)) n.markDeleted()
          else if(added.now.contains(a)) n.markAdded()
          else if(updated.now.contains(a)) n.markChanged()
        n
    }
    agentNodes() = agentNodes.now.filterNot(a => agentsRemoved.contains(a.agent)) ++ addedNodes
  }

  def onNodesChanges(removed: Set[Node], added: Set[Node]): Unit = {
    //removed.foreach(r=> dom.console.log("REMOVED NODE "+r.view.label))
    //added.foreach(a=> dom.console.log("ADDED NODE "+a.view.label))

    removed.foreach{ case n =>
      n.view.clearChildren()
      viz.removeSprite(n.view.container)
      viz.removeObject(n.view.container)
    }
    added.foreach(n => viz.addSprite(n.view.container))

    val es: List[Edge] = added.foldLeft(List.empty[Edge]){case (acc, node) => foldEdges(acc, node)}
    if(added.nonEmpty || removed.nonEmpty)
      edges() = edges.now.filterNot(e => removed.contains(e.from) || removed.contains(e.to)) ++ es
  }


  def onEdgesChanges(removed: List[Edge], added: List[Edge]): Unit = {
    for(r <- removed) r match {
      case link: KappaLinkEdge =>
        link.view.clearChildren()
        viz.removeSprite(link.view.container)
        viz.removeObject(link.view.container)
        viz.removeObject(link.arrow)
      case arr: ArrowEdge => viz.removeObject(arr.arrow)
      case _ => dom.console.log("weird edge")
    }
    for(a <- added) a match {
      case link: KappaLinkEdge =>
        viz.addSprite(link.view.container)
        viz.addObject(link.arrow)
      case arr: ArrowEdge => viz.addObject(arr.arrow)
      case _ => dom.console.log("weird edge")
    }
  }

  protected def createLinkEdges(mp: Map[String, List[SiteNode]]): List[Edge] = {
    mp.collect{
      //case (key, site::Nil) => //let us skip this for the sake of simplicity
      case (key, site1::site2::Nil) =>   new KappaLinkEdge(key, site1, site2, visualSettings.link.line)(createLinkView): Edge
      //case (key, other) => throw  new Exception(s"too many sites for the link $key ! Sights are: $other")
    }.toList
  }

  protected def onLinkChanged(removed: Map[String, List[SiteNode]], added: Map[String, List[SiteNode]], updated: Map[String, (List[SiteNode], List[SiteNode])]) = {

    edges() = edges.now.filterNot{
      case edge: KappaLinkEdge => removed.contains(edge.link.label) || updated.contains(edge.link.label)
      case other => false
    } ++ createLinkEdges(added) ++ createLinkEdges(updated.mapValues(_._2))

  }

  protected def foldEdges(acc: List[Edge], node: Node): List[Edge] = {
    node match {
      case n: SiteNode => new KappaSiteEdge(n.parent, n, visualSettings.link.line) :: acc
      case n: StateNode => new KappaStateEdge(n.parent, n, visualSettings.link.line) :: acc
      case _ => acc //note: we process link edges separately
    }
  }

  protected def subscribeUpdates() = {
    agents.updates.foreach(upd => onAgentsUpdate(upd.removed, upd.added))
    allNodes.updates.foreach{
      case upd => onNodesChanges(upd.removed, upd.added)
    }
    links.updates.foreach{
      case upd =>
        onLinkChanged(upd.removed, upd.added, upd.updated)
    }
    edges.removedInserted.foreach{case (rem, inserted)=> onEdgesChanges(rem.toList, inserted.toList)}
    removed.foreach(r=> agentNodes.now.foreach(n=>if(r.contains(n.agent)) n.markDeleted()))
    updated.foreach(u=> agentNodes.now.foreach(n=>if(u.contains(n.agent)) n.markChanged()))
    added.foreach(r=> agentNodes.now.foreach(n=>if(r.contains(n.agent)) n.markAdded()))
    layouts.now.foreach(_.start(viz.width, viz.height, viz.camera))
  }


  override def bindView() = {
    super.bindView()
    subscribeUpdates()
    viz.render()
  }
}