package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.Agent
import org.denigma.kappa.notebook.layouts._
import org.denigma.kappa.notebook.views.visual.utils.LineParams
import org.scalajs.dom
import org.scalajs.dom.raw.{ClientRect, Element}
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import org.denigma.kappa.notebook.extensions._
import rx.Rx.Dynamic

import scala.List
import scala.Predef.Map
import scala.collection.immutable._

case class KappaNodeVisualSettings(font: Double, padding: Double)
case class KappaEdgeVisualSettings(font: Double, padding: Double, line: LineParams)

//new KappaAgentView(agent.name, 24.0, 10, s )

case class VisualSettings(
                           canvas: SVG,
                           agent: KappaNodeVisualSettings = KappaNodeVisualSettings(26, 8),
                           sites: KappaNodeVisualSettings  = KappaNodeVisualSettings(16, 6),
                           state: KappaNodeVisualSettings = KappaNodeVisualSettings(14, 4),
                           link: KappaEdgeVisualSettings = KappaEdgeVisualSettings(14, 4, LineParams()),
                           otherArrows: LineParams = LineParams()
                         )


class GraphView(val elem: Element,
                val agents: Rx[SortedSet[Agent]],
                val containerName: String,
                val visualSettings: VisualSettings
               ) extends BindableView {

  lazy val size: (Double, Double) = elem.getBoundingClientRect() match {
    case rect: ClientRect if rect.width < 100 || rect.height < 100 => (400.0, 400.0)
    case rect: ClientRect => (rect.width, rect.height)
  }

  val width: Var[Double] = Var(size._1)

  val height: Var[Double] = Var(size._2)

  val container = sq.byId(containerName).get

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

  protected val gravityForce = new Gravity[KappaNode, KappaEdge](ForceLayoutParams.default2D.attractionMult / 4, ForceLayoutParams.default2D.gravityMult, ForceLayoutParams.default2D.center)
  protected val repulsionForce = new Repulsion[KappaNode, KappaEdge](ForceLayoutParams.default2D.repulsionMult, 0.00001, compareRepulsion)
  protected val attractionForce = new Attraction[KappaNode, KappaEdge](ForceLayoutParams.default2D.attractionMult, 0.00001, compareSpring[KappaEdge])
  protected val borderForce = new BorderForce[KappaNode, KappaEdge](ForceLayoutParams.default2D.repulsionMult / 5, 10, 0.9, ForceLayoutParams.default2D.center)

  protected def compareRepulsion(node1: KappaNode, node2: KappaNode): (Double, Double) = (node1, node2) match {
    case (from: AgentNode, to: SiteNode) => (0.5, 0.5)
    case (from: SiteNode, to: AgentNode) => (0.5, 0.5)
    case (from: AgentNode, to: AgentNode) =>(1, 1)
    case (from: SiteNode, to: StateNode) => (0.4, 0.4)
    case (from: StateNode, to: SiteNode) => (0.2, 0.2)
    case (from: SiteNode, to: SiteNode) => (0.5, 0.5)
    case other => (1, 1)
  }

  protected def compareSpring[Edge<: KappaEdge](node1: Edge#FromNode, node2: Edge#ToNode): (Double, Double) = (node1, node2) match {
    case (from: AgentNode, to: SiteNode) => (1, 1)
    case (from: SiteNode, to: AgentNode) => (1, 1)
    case (from: AgentNode, to: AgentNode) =>(1, 1)
    case (from: SiteNode, to: SiteNode) =>(1, 1)
    case (from: SiteNode, to: StateNode) =>(1, 1)
    case (from: StateNode, to: SiteNode) =>(1, 1)
    case other => (1, 1)
  }

  protected val forces: Vector[Force[ KappaNode, KappaEdge]] = Vector(
    repulsionForce,
    attractionForce,
    gravityForce,
    borderForce
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


  val allNodes: Rx[Set[KappaNode]] = Rx{
    agentNodes() ++ siteNodes() ++ stateNodes()//++ links()
  }

  val nodes: Rx[Vector[KappaNode]] = allNodes.map(n=>n.toVector)//Var(Vector.empty[KappaNode])

  val edges = Var(Vector.empty[KappaEdge])

  val layouts = Var(Vector(new RulesForceLayout(nodes, edges, ForceLayoutParams.default2D.mode, forces)))

  val viz = new Visualizer(container,
    width,
    height,
    layouts,
    700.0
  )
  protected def onAgentsUpdate(removed: Set[Agent], added: Set[Agent]) = {
    val addedNodes = added.map(a => new AgentNode(a))
    agentNodes() = agentNodes.now.filterNot(a => removed.contains(a.agent)) ++ addedNodes
  }

  def onNodesChanges(removed: Set[KappaNode], added: Set[KappaNode]): Unit = {
    removed.foreach(r=> dom.console.log("REMOVED NODE "+r.view.label))
    added.foreach(a=> dom.console.log("ADDED NODE "+a.view.label))

    removed.foreach{ case n =>
      n.view.clearChildren()
      viz.removeSprite(n.view.container)
      viz.removeObject(n.view.container)
    }
    added.foreach(n => viz.addSprite(n.view.container))

    val es: List[KappaEdge] = added.foldLeft(List.empty[KappaEdge]){case (acc, node) => foldEdges(acc, node)}
    if(added.nonEmpty || removed.nonEmpty)
      edges() = edges.now.filterNot(e => removed.contains(e.from) || removed.contains(e.to)) ++ es
  }


  def onEdgesChanges(removed: List[KappaEdge], added: List[KappaEdge]): Unit = {
    dom.console.log("edge changed!")
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

  protected def createLinkEdges(mp: Map[String, List[SiteNode]]): List[KappaEdge] = {
    mp.collect{
      //case (key, site::Nil) => //let us skip this for the sake of simplicity
      case (key, site1::site2::Nil) =>   new KappaLinkEdge(key, site1, site2)(createLinkView): KappaEdge
      //case (key, other) => throw  new Exception(s"too many sites for the link $key ! Sights are: $other")
    }.toList
  }

  protected def onLinkChanged(removed: Map[String, List[SiteNode]], added: Map[String, List[SiteNode]], updated: Map[String, (List[SiteNode], List[SiteNode])]) = {

    edges() = edges.now.filterNot{
      case edge: KappaLinkEdge => removed.contains(edge.link.label) || updated.contains(edge.link.label)
      case other => false
    } ++ createLinkEdges(added) ++ createLinkEdges(updated.mapValues(_._2))

  }

  protected def foldEdges(acc: List[KappaEdge], node: KappaNode): List[KappaEdge] = {
    node match {
      case n: SiteNode => new KappaSiteEdge(n.parent, n) :: acc
      case n: StateNode => new KappaStateEdge(n.parent, n) :: acc
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
    edges.removedInserted.foreach{case (removed, inserted)=> onEdgesChanges(removed.toList, inserted.toList)}
    layouts.now.foreach(_.start(viz.width, viz.height, viz.camera))
  }


  override def bindView() = {
    super.bindView()
    subscribeUpdates()
    viz.render()
  }
}