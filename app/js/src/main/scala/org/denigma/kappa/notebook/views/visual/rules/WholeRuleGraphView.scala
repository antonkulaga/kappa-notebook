package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.binding.extensions._
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.model.KappaModel.{Site, State}
import org.denigma.kappa.model.{Change, KappaModel}
import org.denigma.kappa.notebook.actions.Commands
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.layouts._
import org.denigma.kappa.notebook.views.visual.ShowParameters
import org.denigma.kappa.parsers.GraphUpdateInfo
import org.scalajs.dom
import org.scalajs.dom.raw.{ClientRect, Element, HTMLElement}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.Predef.{Map, Set}
import scala.collection.immutable._
import scala.{List, Vector}

/**
  * View for rules visualizations
  * @param elem html element to bind to
  * @param update GraphUpdateInfo provide nice information about differences between left and right sides
  * @param showState
  * @param containerName name of the element to draw 3D to
  * @param visualSettings some display settings
  * @param input input is used to subscribe to UI and incoming websocket events
  */
class WholeRuleGraphView(val elem: Element,
                         val update: Rx[GraphUpdateInfo],
                         val showState: Rx[ShowParameters.ShowParameters],
                         val containerName: String,
                         val visualSettings: RulesVisualSettings,
                         val input: Var[KappaMessage]
                        ) extends VisualGraph
{

  lazy val size: (Double, Double) = elem.getBoundingClientRect() match {
    case rect: ClientRect if rect.width < 100 || rect.height < 100 => (400.0, 400.0)
    case rect: ClientRect => (rect.width, rect.height)
  }
  val width: Var[Double] = Var(size._1)

  val height: Var[Double] = Var(size._2)

  lazy val nodes = Var(Vector.empty[Node])

  lazy val edges = Var(Vector.empty[Edge])

  lazy val container: HTMLElement = sq.byId(containerName).get

  protected def makeLinkEdge(siteFrom: String, fromPos:Int, siteTo: String, toPos:Int, source: Map[(String, Int), SiteNode], status: Change.Change)
                            (implicit getLineParams: Change.Change => LineParams): Option[KappaLinkEdge] = {
    (source.get((siteFrom, fromPos)), source.get(siteTo, toPos)) match {
      case (Some(site1), Some(site2)) => Some(new KappaLinkEdge(site1, site2, status, getLineParams(status)))
      case other =>
        dom.console.error(s"cannot find ${other} with ${siteFrom} ${fromPos} ${siteTo} ${toPos} in ${source}")
        None
    }
  }

  lazy val minSpring = 75

  def massByNode(node: KappaNode): Double = node match {
    case n: AgentNode => 1.4
    case s: SiteNode => 1.0
    case st: StateNode => 0.4
  }

  protected def computeSpring(edge: Edge): SpringParams = (edge.from, edge.to) match {
    case (from: SiteNode, to: SiteNode) => SpringParams(minSpring * 1.4, 1.4, massByNode(from), massByNode(to))
    case (from: SiteNode, to: AgentNode) => SpringParams(minSpring, 1, massByNode(from), massByNode(to))
    case (from: AgentNode, to: SiteNode) => SpringParams(minSpring, 1, massByNode(from), massByNode(to))
    case (from: AgentNode, to: AgentNode) => SpringParams(minSpring, 2, massByNode(from), massByNode(to))
    case (from: KappaNode, to: KappaNode) => SpringParams(minSpring, 1, massByNode(from), massByNode(to))
  }

  val forceLayoutParams = Var(ForceLayoutParams.default2D)

  protected lazy val forces: Rx[Vector[Force[ Node, Edge]]] = Rx{
    val params = forceLayoutParams()
    val repulsionForce = new Repulsion[Node, Edge](params.repulsionMult)(compareRepulsion)
    val springForce = new SpringForce[Node, Edge](params.springMult)(computeSpring)
    val gravityForce = new Gravity[Node, Edge](params.gravityMult, params.center)
    Vector(
      repulsionForce,
      springForce,
      gravityForce
    )
  }

  protected lazy val mode = forceLayoutParams.map(p=>p.mode)


  protected def compareRepulsion(node1: Node, node2: Node): (Double, Double) = (massByNode(node1), massByNode(node2))


  lazy val iterationsPerFrame = Var(10)//Var(1) //Var(10)
  lazy val firstFrameIterations = Var(300)//Var(0) //Var(300)


  val viz = new Visualizer(container,
    width,
    height,
    layouts,
    850,
    iterationsPerFrame,
    firstFrameIterations
  )


  lazy val layouts: Rx[Vector[GraphLayout]] = Rx{
    val layout = new RulesForceLayout(nodes, edges, mode(), forces())
    Vector(layout)
  }


  input.onChange{
    case Commands.SetLayoutParameters("rules", params) =>
      forceLayoutParams() = params

    case other => //do nothing
  }

  implicit def lineParamsByStatus(status: Change.Change): LineParams = status match {
    case Change.Removed => LineParams(Colors.red)
    case Change.Added => LineParams(Colors.green)
    case _ => LineParams(Colors.blue)
  }

  implicit def getLineParams(from: KappaNode, to: KappaNode) = (from , to) match {
    case (f: ChangeableNode, t: ChangeableNode) if f.status == Change.Removed || t.status == Change.Removed =>
      LineParams(Colors.red)
    case (f: ChangeableNode, t: ChangeableNode) if f.status == Change.Added || t.status == Change.Added =>
      LineParams(Colors.green)
    case _ => LineParams(Colors.blue)
  }

  protected def mergeNodes(left: KappaModel.Agent, right: KappaModel.Agent): AgentNode = {
    if(left==right) {
      AgentNode(left, Change.Unchanged)
    } else {
      val children = mergeNodeSites(left, right)
      new AgentNode(left, children, Change.Updated)
    }
  }

  protected def mergeNodeSites(left: KappaModel.Agent, right: KappaModel.Agent): Map[Change.Change, Set[SiteNode]] = {
    val sameNames = left.siteNames.intersect(right.siteNames)
    val all = left.sites.toList ++ right.sites.toList
    val grouped = all.groupBy(s=>s.name).toList
    val positions = List(left.position, right.position)
    val unchangedNodes: List[SiteNode] = grouped.collect{
      case (name, one::two::tail) if one==two =>
        if(tail.nonEmpty) dom.console.error("tail is not empty, sites are: " + one::two::tail)
        val links = OrganizedChangeableNode.emptyChangeMap[String].updated(Change.Unchanged, one.links)
        SiteNode(left, one, Change.Unchanged, positions)
    }
    val updatedNodes: List[SiteNode] = grouped.collect{
      case (name, one::two::tail) if one!=two  =>
        if(tail.nonEmpty) dom.console.error("tail is not empty, sites are: " + one::two::tail)     
        val states =  mergeSiteStates(one, two)
        new SiteNode(left, one, states, Change.Updated, positions)
    }
    val removed: Set[Site] = left.sites.filterNot(s => sameNames.contains(s.name))
    val added: Set[Site] = right.sites.filterNot(s => sameNames.contains(s.name))
    val addedNodes: Set[SiteNode] = added.map{ a =>
        SiteNode(left, a, Change.Added, positions)
    }
    val removedNodes: Set[SiteNode] = removed.map{ r =>
        val links =  OrganizedChangeableNode.emptyChangeMap[String].updated(Change.Removed, r.links)
        SiteNode(left, r, Change.Removed, positions)
    }
    Map(
      (Change.Removed , removedNodes),
      (Change.Added , addedNodes),
      (Change.Unchanged , unchangedNodes.toSet),
      (Change.Updated , updatedNodes.toSet)
    )
  }

  protected def mergeSiteStates(left: KappaModel.Site, right: KappaModel.Site): Map[Change.Change, Set[StateNode]] = {
    val removed: Set[State] = left.states.diff(right.states)
    val removedNodes: Set[StateNode] = removed.map(r => StateNode(left, r, Change.Removed))
    val added = right.states.diff(left.states)
    val addedNodes: Set[StateNode] = added.map(a => StateNode(left, a, Change.Added))
    val unchanged: Set[State] = left.states.intersect(right.states)
    val unchangedNodes: Set[StateNode] = unchanged.map(u => StateNode(left, u, Change.Unchanged))
    Map(
      (Change.Removed , removedNodes),
      (Change.Added , addedNodes),
      (Change.Unchanged , unchangedNodes),
      (Change.Updated , Set.empty[StateNode])
    )
  }

  def onNodesChanges(removed: Set[Node], added: Set[Node]): Unit = {
   removed.foreach{ n =>
      n.view.clearChildren()
      viz.removeSprite(n.view.container)
      //viz.removeObject(n.view.container)
    }
    added.foreach{
      n =>
        updateNodeVisibility(n, showState.now)
        viz.addSprite(n.view.container)
    }
  }

  def onEdgesChanges(removed: Set[Edge], added: Set[Edge]): Unit = {
    for(r <- removed) r match {
      case link: KappaLinkEdge => viz.removeObject(link.line)
      case arr: Edge => viz.removeObject(arr.line)
      case _ => dom.console.log("weird edge")
    }
    for(a <- added) a match {
      case link: KappaLinkEdge => viz.addObject(link.line)
      case arr: Edge => viz.addObject(arr.line)
      case _ => dom.console.log("weird edge")
    }
  }

  protected def createLinkEdges(mp: Map[(String, Change.Change), Vector[SiteNode]]): Vector[Edge] = {
    mp.collect{
      //case (key, site::Nil) => //let us skip this for the sake of simplicity
      case ((name, change), value)  if value.size > 1 =>
        if(value.size > 2) {
          dom.console.error("THE NUMBER OF THE SAME LINK FOR THE SAME CHANGE TYPE SHOULD NOT BE MORE THAN 2")
          dom.console.error(s"sit is $name change is $change links are :" + value.toList.map(v=>v.site))
        }
        val (site1, site2) = (value(0), value(1))
        val lineParams = linkLineByStatus(change, visualSettings.link.line)
        new KappaLinkEdge(site1, site2, change, lineParams): Edge
      //case (key, other) => throw  new Exception(s"too many sites for the link $key ! Sights are: $other")
    }.toVector
  }

  protected def linkLineByStatus(status: Change.Change, line: LineParams) = status match {
    case Change.Added => line.copy(lineColor = Colors.green)
    case Change.Removed => line.copy(lineColor = Colors.red)
    case _ => line.copy(lineColor = Colors.blue  )
  }

  protected def onUpdate(updateInfo: GraphUpdateInfo) = {
    val added  = updateInfo.addedAgents.map(a => AgentNode(a, Change.Added))
    val removed: Set[AgentNode] = updateInfo.removedAgents.map(a => AgentNode(a, Change.Removed))
    val unchanged: Set[AgentNode] = updateInfo.unchangedAgents.map(a => AgentNode(a, Change.Unchanged))
    val merged: Set[AgentNode] = updateInfo.updatedAgents.map{ case (one, two) => mergeNodes(one, two)}

    val removedSites =  removed.flatMap(a => a.childrenList)
    val addedSites =  added.flatMap(a => a.childrenList)
    val unchangedSites =  unchanged.flatMap(a => a.childrenList)
    val mergedSites = merged.flatMap(a => a.childrenList)

    val allAgentNodes = added ++ removed ++ unchanged ++ merged
    val allSiteNodes = addedSites ++ removedSites ++ unchangedSites ++ mergedSites
    val allStateNodes: Set[StateNode] = allSiteNodes.flatMap(s => s.childrenList)
    val allNodes: Set[Node] = allAgentNodes ++ allSiteNodes ++ allStateNodes

    nodes() = allNodes.toVector //UPDATE NODES


    val sameSites = unchangedSites ++ mergedSites
    val leftSites: Set[SiteNode] =removedSites ++ sameSites
    val rightSites: Set[SiteNode] = addedSites ++ sameSites //note here might be a big mistake!!!
    val sitesOnLeft: Map[(String, Int), SiteNode] = leftSites.map{ s => ((s.site.name, s.agentPositions.head) , s) }.toMap

    val sitesOnRight: Map[(String, Int), SiteNode]  = rightSites.map{
        case s if s.agentPositions.size > 1=> ( (s.site.name, s.agentPositions.tail.head) , s)
        case s => ((s.site.name, s.agentPositions.head) , s) //TODO: fix this unsafe code
    }.toMap

    val removedLinks = updateInfo.removedLinks.map{
      case (fromName, fromPos, toName, toPos) =>
        val e = makeLinkEdge(fromName, fromPos, toName, toPos, sitesOnLeft, Change.Removed)(lineParamsByStatus)
        if(e.isEmpty) dom.console.error("e is empty!")
        e.get
    }

    val addedLinks = updateInfo.addedLinks.map{
      case (fromName, fromPos, toName, toPos) =>
        val e = makeLinkEdge(fromName, fromPos, toName, toPos, sitesOnRight, Change.Added)(lineParamsByStatus)
        if(e.isEmpty) dom.console.error("e is empty!")
        e.get
    }

    val unchangedLinks = updateInfo.unchangedLinks.map{
      case (fromName, fromPos, toName, toPos) =>
        val e = makeLinkEdge(fromName, fromPos, toName, toPos, sitesOnLeft, Change.Unchanged)(lineParamsByStatus)
        if(e.isEmpty) dom.console.error("e is empty!")
        e.get
    }

    val allLinks = removedLinks ++ addedLinks ++ unchangedLinks

    val allChildEdges = allAgentNodes.flatMap(a => a.allChildEdges)

    edges() = (allLinks ++ allChildEdges).toVector //UPDATE EDGES

  }


  protected def subscribeUpdates() = {
    showState.foreach{
      sh =>
        nodes.now.foreach(n => updateNodeVisibility(n, sh))
        edges.now.foreach(e => updateEdgeVisibility(e, sh))
    }
    nodes.removedInserted.foreach{ case (r, a) => onNodesChanges(r.toSet, a.toSet) }
    edges.removedInserted.foreach{ case (r, a) => onEdgesChanges(r.toSet, a.toSet) }
    update.foreach(onUpdate)

    layouts.now.foreach(_.start(viz.width, viz.height, viz.camera))
  }

  //bad code, TODO: fix it
  protected def updateEdgeVisibility(edge: KappaEdge, show: ShowParameters.ShowParameters) = (edge, show) match {
    case (e : Edge, ShowParameters.Left) =>
      e.status match {
        case Change.Added =>  e.opacity = 0.15
        case Change.Removed =>   e.opacity = 1.0
        case Change.Updated =>   e.opacity = 1.0
        case Change.Unchanged =>   e.opacity = 1.0
      }

    case (e : Edge, ShowParameters.Right) =>
      e.status match {
        case Change.Added =>  e.opacity = 1.0
        case Change.Removed =>   e.opacity = 0.15
        case Change.Updated =>   e.opacity = 1.0
        case Change.Unchanged =>   e.opacity = 1.0
      }

    case (e : Edge, ShowParameters.Both) =>
      e.status match {
        case Change.Added =>  e.opacity = 1.0
        case Change.Removed =>   e.opacity = 1.0
        case Change.Updated =>   e.opacity = 1.0
        case Change.Unchanged =>   e.opacity = 1.0
      }

  }

  // bad code, TODO: fix it
  protected def updateNodeVisibility(node: Node, show: ShowParameters.ShowParameters) = (node, show) match {
    case (n: ChangeableNode, ShowParameters.Left) =>
      n.status match {
        case Change.Added =>  n.view.opacity = 0.15
        case Change.Removed =>   n.view.opacity = 1.0
        case Change.Updated =>   n.view.opacity = 1.0
        case Change.Unchanged =>   n.view.opacity = 1.0
      }

    case (n: ChangeableNode, ShowParameters.Right) =>
      n.status match {
        case Change.Added =>  n.view.opacity = 1.0
        case Change.Removed =>   n.view.opacity = 0.15
        case Change.Updated =>   n.view.opacity = 1.0
        case Change.Unchanged =>   n.view.opacity = 1.0
      }

    case (n: ChangeableNode, ShowParameters.Both) =>
      n.status match {
        case Change.Added =>  n.view.opacity = 1.0
        case Change.Removed =>   n.view.opacity = 1.0
        case Change.Updated =>   n.view.opacity = 1.0
        case Change.Unchanged =>   n.view.opacity = 1.0
      }

    case _ => dom.console.info(s"unknow node $node")
  }


  override def bindView() = {
    super.bindView()
    subscribeUpdates()
    viz.render()
  }

}