package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.binding.views.BindableView
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.layouts._
import org.scalajs.dom.svg.LinearGradient
import rx._

import scala.Vector
import scala.collection.immutable._
import scalatags.JsDom.TypedTag
import rx.Ctx.Owner.Unsafe.Unsafe

object Gradients {
  import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all._
  import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all.attrs._

  def blueGradient(gradientName: String): TypedTag[LinearGradient] =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "skyblue"),
      stop(offset := "50%", stopColor := "deepskyblue"),
      stop(offset := "100%", stopColor := "SteelBlue")
    )

  def lightBlueGradient(gradientName: String): TypedTag[LinearGradient]  =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )
  /*
      <linearGradient id="grad1" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="skyblue" />
      <stop offset="50%" stop-color="deepskyblue" />
      <stop offset="100%" stop-color="SteelBlue" />
     </linearGradient>
   */

  def redGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#ff9999"),
      stop(offset := "50%", stopColor := "#ff6666"),
      stop(offset := "100%", stopColor := "#ff6666")
    )
  }

  def greenGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#adebad"),
      stop(offset := "50%", stopColor := "#40bf40"),
      stop(offset := "100%", stopColor := "#609f60")
    )
  }

  def purpleGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#e6b3ff"),
      stop(offset := "50%", stopColor := "#d580ff"),
      stop(offset := "100%", stopColor := "#b31aff")
    )
  }

  def gradientByStatus(status: Change.Change, gradientName: String) = status match {
    case Change.Removed => Gradients.redGradient(gradientName)
    case Change.Added => Gradients.greenGradient(gradientName)
    case Change.Unchanged | Change.Updated =>
      if(gradientName == KappaSiteView.gradientName)  Gradients.lightBlueGradient(gradientName) else Gradients.blueGradient(gradientName)
    //case Change.Updated => if(gradientName == KappaSiteView.gradientName)  Gradients.purpleGradient(gradientName) else Gradients.purpleGradient(gradientName)

  }
}


trait  RuleGraphWithForces extends BindableView{

  type Node = KappaNode

  type Edge = KappaEdge

  def visualSettings: RulesVisualSettings

  implicit protected def createAgentNodeView(agent: AgentNode): KappaAgentView = {
    val gradient = Gradients.gradientByStatus(agent.status, KappaAgentView.gradientName)
    new KappaAgentView(agent.agent.name, visualSettings.agent.font, visualSettings.agent.padding, gradient, visualSettings.canvas)
  }

  implicit protected def createSiteNodeView(site: SiteNode): KappaSiteView = {
    val gradient = Gradients.gradientByStatus(site.status, KappaSiteView.gradientName)
    new KappaSiteView(site.site.name, visualSettings.sites.font, visualSettings.sites.padding, gradient, visualSettings.canvas)
  }

  implicit protected def createStateNodeView(state: StateNode): KappaStateView = {
    val gradient = Gradients.gradientByStatus(state.status, KappaStateView.gradientName)
    new KappaStateView(state.state.name, visualSettings.state.font, visualSettings.state.padding, gradient, visualSettings.canvas)
  }

  implicit protected def createLinkView(edge: KappaLinkEdge): KappaLinkView = {
    val gradient = Gradients.gradientByStatus(edge.status, KappaLinkView.gradientName)
    new KappaLinkView(edge.link.label, visualSettings.link.font, visualSettings.link.padding, gradient, visualSettings.canvas)
  }

  lazy val minSpring = 90

  def massByNode(node: KappaNode): Double = node match {
    case n: AgentNode => 1.5
    case s: SiteNode => 1.0
    case st: StateNode => 0.8
  }

  protected def computeSpring(edge: Edge): SpringParams = (edge.from, edge.to) match {
    case (from: SiteNode, to: AgentNode) => SpringParams(minSpring, 1.5, massByNode(from), massByNode(to))
    case (from: AgentNode, to: SiteNode) => SpringParams(minSpring, 1.5, massByNode(from), massByNode(to))
    case (from: AgentNode, to: AgentNode) => SpringParams(minSpring, 2, massByNode(from), massByNode(to))
    case (from: KappaNode, to: KappaNode) => SpringParams(minSpring, 1, massByNode(from), massByNode(to))
  }

  protected lazy val forces: Vector[Force[ Node, Edge]] = Vector(
    repulsionForce,
    springForce
    //gravityForce
    //,borderForce
  )
  //protected val gravityForce = new Gravity[Node, Edge](ForceLayoutParams.default2D.attractionMult / 4, ForceLayoutParams.default2D.gravityMult, ForceLayoutParams.default2D.center)
  protected lazy val repulsionForce: Repulsion[Node, Edge] = new Repulsion[Node, Edge](ForceLayoutParams.default2D.repulsionMult)(compareRepulsion)
  protected lazy val springForce: SpringForce[Node, Edge] = new SpringForce[Node, Edge](ForceLayoutParams.default2D.springMult)(computeSpring)
  protected lazy val borderForce: BorderForce[Node, Edge] = new BorderForce[Node, Edge](ForceLayoutParams.default2D.repulsionMult / 5, 10, 0.9, ForceLayoutParams.default2D.center)

  protected def compareRepulsion(node1: Node, node2: Node): (Double, Double) = (massByNode(node1), massByNode(node2))


  lazy val iterationsPerFrame = Var(5)
  lazy val firstFrameIterations = Var(50)

  def layouts: Var[Vector[GraphLayout]]
}
/*

trait RulesGraphView extends BindableView with RuleGraphWithForces {

  def unchanged: Rx[Set[Agent]]
  def removed: Rx[Set[Agent]]
  def added: Rx[Set[Agent]]
  def updated: Rx[Set[Agent]]


  def width: Rx[Double]

  def height: Rx[Double]

  def container: HTMLElement

  val agents: Rx[Set[Agent]] = Rx{
    unchanged() ++ removed() ++ added() ++ updated()
  }

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

  lazy val layouts: Var[Vector[GraphLayout]] = Var(Vector(new RulesForceLayout(nodes, edges, ForceLayoutParams.default2D.mode, forces)))

  def viz: Visualizer

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
*/