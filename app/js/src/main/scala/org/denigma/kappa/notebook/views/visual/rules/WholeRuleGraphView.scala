package org.denigma.kappa.notebook.views.visual.rules


import org.denigma.binding.extensions._
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{Agent, Site, State}
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.layouts.{ForceLayoutParams, GraphLayout}
import org.scalajs.dom
import org.scalajs.dom.raw.{ClientRect, Element, HTMLElement}
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.{List, Vector}
import scala.Predef.{Map, Set}
import scala.collection.immutable._

/*
class WholeRuleGraphView(val elem: Element,
                         val unchanged: Rx[Set[Agent]],
                         val removed: Rx[Set[Agent]],
                         val added: Rx[Set[Agent]],
                         val updated: Rx[Set[Agent]],
                         val containerName: String,
                         val visualSettings: RulesVisualSettings
                        ) extends RulesGraphView {

  lazy val size: (Double, Double) = elem.getBoundingClientRect() match {
    case rect: ClientRect if rect.width < 100 || rect.height < 100 => (400.0, 400.0)
    case rect: ClientRect => (rect.width, rect.height)
  }
  val width: Var[Double] = Var(size._1)

  val height: Var[Double] = Var(size._2)

  override lazy val container: HTMLElement = sq.byId(containerName).get
  
  val viz = new Visualizer(container,
    width,
    height,
    layouts,
    750.0,
    iterationsPerFrame,
    firstFrameIterations
  )
}
*/

class WholeRuleGraphView(val elem: Element,
                         val unchanged: Rx[Set[Agent]],
                         val removed: Rx[Set[Agent]],
                         val added: Rx[Set[Agent]],
                         val updated: Rx[Set[(Agent, Agent)]],
                         val containerName: String,
                         val visualSettings: RulesVisualSettings
                        ) extends RuleGraphWithForces
{

  lazy val size: (Double, Double) = elem.getBoundingClientRect() match {
    case rect: ClientRect if rect.width < 100 || rect.height < 100 => (400.0, 400.0)
    case rect: ClientRect => (rect.width, rect.height)
  }
  val width: Var[Double] = Var(size._1)

  val height: Var[Double] = Var(size._2)

  lazy val nodes = Var(Vector.empty[KappaNode])
  lazy val edges = Var(Vector.empty[KappaEdge])

  lazy val container: HTMLElement = sq.byId(containerName).get

  val allAgentNodes: Var[Set[AgentNode]] = Var(Set.empty[AgentNode])

  lazy val siteNodes = nodes.map(nds => nds.collect{case node: SiteNode => node})

  val links: Rx[Map[String, Vector[SiteNode]]] = siteNodes.map{ case sNodes =>
    sNodes
      .flatMap(node =>  node.site.links.map(l=> (node , l)))
      .groupBy{case (site, link)=> link}
      .mapValues(lst => lst.map{
        case (key, value) => key })
  }



  val viz = new Visualizer(container,
    width,
    height,
    layouts,
    900,
    iterationsPerFrame,
    firstFrameIterations
  )

  implicit def getLineParams(from: KappaNode, to: KappaNode) = (from , to) match {
    case (f: ChangeableNode, t: ChangeableNode) if f.status == Change.Removed || t.status == Change.Removed =>
      LineParams(Colors.red)
    case (f: ChangeableNode, t: ChangeableNode) if f.status == Change.Added || t.status == Change.Added =>
      LineParams(Colors.green)

    case _ => LineParams(Colors.blue)
  }

  def mergeNodes(left: KappaModel.Agent, right: KappaModel.Agent): AgentNode = {
    if(left==right) {
      AgentNode(left, Change.Unchanged)
    } else {
      val children = mergeNodeSites(left, right)
      new AgentNode(left, children, Change.Updated)
    }
  }

  def mergeNodeSites(left: KappaModel.Agent, right: KappaModel.Agent): Map[Change.Change, Set[SiteNode]] = {
    val sameNames = left.siteNames.intersect(right.siteNames)
    val all = left.sites ++ right.sites
    val grouped = all.toList.groupBy(s=>s.name).toList
    val unchangedNodes: List[SiteNode] = grouped.collect{
      case (name, one::two::Nil) if one==two =>
        SiteNode(left, one, Change.Unchanged)
    }
    val updatedNodes: List[SiteNode] = grouped.collect{
      case (name, one::two::Nil) if one==two =>
        SiteNode(left, one, Change.Unchanged)
      case (name, one::two::Nil) =>
        val states =  mergeSiteStates(left, one , two)
        new SiteNode(left, one, states, Change.Updated)
    }
    val removed: Set[Site] = left.sites.filterNot(s=>sameNames.contains(s.name))
    val added: Set[Site] = right.sites.filterNot(s=>sameNames.contains(s.name))
    val addedNodes: Set[SiteNode] = added.map{ case a => SiteNode(left, a, Change.Added) }
    val removedNodes: Set[SiteNode] = removed.map{ case r => SiteNode(left, r, Change.Removed) }
    Map(
      (Change.Removed , removedNodes),
      (Change.Added , addedNodes),
      (Change.Unchanged , unchangedNodes.toSet),
      (Change.Updated , updatedNodes.toSet)
    )
  }


  def mergeSiteStates(parent: Agent, left: KappaModel.Site, right: KappaModel.Site): Map[Change.Change, Set[StateNode]] = {
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


  protected def onAgentUpdates(deleted: Set[Agent], added: Set[Agent], status: Change.Change) = {
      val addedNodes = added.map(a => AgentNode(a, status))
      allAgentNodes() = allAgentNodes.now.filterNot(n => deleted.contains(n.agent)) ++ addedNodes
  }

  protected def getAllAgentNodes(node: AgentNode) = {
    val nds: List[KappaNode] = node :: node.childrenList ++ node.childrenList.flatMap(ch => ch.childrenList)
    nds.toSet
  }

  protected def getAllEdgesFrom(node: AgentNode): Set[KappaEdge] = {
    val edges = node.childEdgeList ++ node.childrenList.flatMap(ch => ch.childEdgeList)
    edges.toSet
  }

  protected def onAgentNodesUpdate(removed: Set[AgentNode], added: Set[AgentNode]) = {
    val toRemove = removed.flatMap{ r => getAllAgentNodes(r)}
    dom.console.log("TO REMOVE COUNT " + toRemove)
    val toAdd = added.flatMap( a => getAllAgentNodes(a))
    dom.console.log("TO ADD COUNT " + toRemove)

    nodes() = nodes.now.filterNot(n => toRemove.contains(n)) ++ toAdd
    val addedEdges = added.flatMap(a => getAllEdgesFrom(a))
    edges() = edges.now.filterNot{ case e => toRemove.exists(a => a== e.from || a == e.to) } ++ addedEdges
  }


  def onNodesChanges(removed: Set[Node], added: Set[Node]): Unit = {
    //removed.foreach(r=> dom.console.log("REMOVED NODE "+r.view.label))
    //added.foreach(a=> dom.console.log("ADDED NODE "+a.view.label))

    removed.foreach{ case n =>
      dom.console.log("REMOVED IS "+n.view.label)
      n.view.clearChildren()
      viz.removeSprite(n.view.container)
      viz.removeObject(n.view.container)
    }
    added.foreach{
      case n =>
        dom.console.log("ADDED IS "+n.view.label)
        viz.addSprite(n.view.container)
    }
    //if(added.nonEmpty || removed.nonEmpty) edges() = edges.now.filterNot(e => removed.contains(e.from) || removed.contains(e.to))
  }


  /*
  protected def foldEdges(acc: List[Edge], node: Node): List[Edge] = {
    node match {
      case n: SiteNode => new KappaSiteEdge(n.parent, n, visualSettings.link.line) :: acc
      case n: StateNode => new KappaStateEdge(n.parent, n, visualSettings.link.line) :: acc
      case _ => acc //note: we process link edges separately
    }
  }
  */

  def onEdgesChanges(removed: Set[Edge], added: Set[Edge]): Unit = {
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
/*
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
  */

  protected def createLinkEdges(mp: Map[String, Vector[SiteNode]]): Vector[Edge] = {
    mp.collect{
      //case (key, site::Nil) => //let us skip this for the sake of simplicity
      case (key, value)  if value.size ==2 =>
        val (site1, site2) = (value(0), value(1))
        new KappaLinkEdge(key, site1, site2, visualSettings.link.line)(createLinkView): Edge
      //case (key, other) => throw  new Exception(s"too many sites for the link $key ! Sights are: $other")
    }.toVector
  }

  protected def onLinkChanged(removed: Map[String, Vector[SiteNode]], added: Map[String, Vector[SiteNode]], updated: Map[String, (Vector[SiteNode], Vector[SiteNode])]) = {

    edges() = edges.now.filterNot{
      case edge: KappaLinkEdge => removed.contains(edge.link.label) || updated.contains(edge.link.label)
      case other => false
    } ++ createLinkEdges(added) ++ createLinkEdges(updated.mapValues(_._2))

  }


  protected def subscribeUpdates() = {
    nodes.removedInserted.foreach{
      case (r, a) => onNodesChanges(r.toSet, a.toSet)
    }
    edges.removedInserted.foreach{
      case (r, a) => onEdgesChanges(r.toSet, a.toSet)
    }

    links.updates.foreach{
      case upd => onLinkChanged(upd.removed, upd.added, upd.updated)
    }

    allAgentNodes.updates.foreach{ case upd =>  onAgentNodesUpdate(upd.removed, upd.added) }
    onAgentNodesUpdate(Set.empty, allAgentNodes.now)

    onAgentUpdates(Set.empty, unchanged.now, Change.Unchanged)
    unchanged.updates.foreach(upd => onAgentUpdates(upd.removed, upd.added, Change.Unchanged))

    onAgentUpdates(Set.empty, removed.now, Change.Removed)
    removed.updates.foreach(upd => onAgentUpdates(upd.removed, upd.added, Change.Removed))

    onAgentUpdates(Set.empty, added.now, Change.Added)
    added.updates.foreach(upd => onAgentUpdates(upd.removed, upd.added, Change.Added))

    //onNodesUpdates(Set.empty, mergedAgents.now, Change.Updated)
    //mergedAgents.updates.foreach(upd => onNodesUpdates(upd.removed, upd.added, Change.Updated))
    /*
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
   */
    layouts.now.foreach(_.start(viz.width, viz.height, viz.camera))
  }


  override def bindView() = {
    super.bindView()
    subscribeUpdates()
    viz.render()
  }

  lazy val layouts: Var[Vector[GraphLayout]] = Var(Vector(new RulesForceLayout(nodes, edges, ForceLayoutParams.default2D.mode, forces)))

}