package org.denigma.kappa.notebook.views.visual.rules


import org.denigma.binding.binders.Events
import org.denigma.binding.extensions._
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{Agent, Site, State}
import org.denigma.kappa.notebook.graph.Change.Change
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.layouts.{ForceLayoutParams, GraphLayout}
import org.scalajs.dom
import org.scalajs.dom.raw.{ClientRect, Element, HTMLElement}
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

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

object RuleViewMode extends Enumeration {
  type Change = Value
  val Left, Right, Both = Value
}

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

  lazy val siteNodes: Rx[Vector[SiteNode]] = nodes.map(nds => nds.collect{case node: SiteNode => node})

  val links: Rx[Map[String, Vector[(Change, SiteNode)]]] = siteNodes.map{ case sNodes =>
    sNodes.foreach(s=>dom.console.log(s" SITE ${s.site} LINKS = "+ s.links))
    val lns = sNodes
      .flatMap(snode => snode.linkList.map(ls => (ls , snode)))
      .groupBy{ case ((site, change), nds) => site }
      .mapValues(vector => vector.map { case ((site, change), node) => (change , node) })
    lns.foreach{
      case (key, vector) =>
        println(s"LINK ${key} with vector ${vector.map(v => v._2.site).toList.mkString(" || ")}")
    }
    lns
  }

  lazy val merged = updated.map{
    case up => up.map{  case (one, two) => mergeNodes(one, two) }
  }

  val viz = new Visualizer(container,
    width,
    height,
    layouts,
    1000,
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

  protected def mergeNodes(left: KappaModel.Agent, right: KappaModel.Agent): AgentNode = {
    if(left==right) {
      AgentNode(left, Change.Unchanged)
    } else {
      val children = mergeNodeSites(left, right)
      new AgentNode(left, children, Change.Updated)
    }
  }

  protected def mergeLinks(left: Site, right: Site): Map[Change.Change, Set[String]] = {
    val same = left.links.intersect(right.links)
    val removed = left.links.diff(same)
    val added = right.links.diff(same)
    Map( (Change.Removed , removed), (Change.Added , added), (Change.Unchanged , same), (Change.Updated , Set.empty[String]) )
  }

  protected def mergeNodeSites(left: KappaModel.Agent, right: KappaModel.Agent): Map[Change.Change, Set[SiteNode]] = {
    val sameNames = left.siteNames.intersect(right.siteNames)
    val all = left.sites ++ right.sites
    val grouped = all.toList.groupBy(s=>s.name).toList
    val unchangedNodes: List[SiteNode] = grouped.collect{
      case (name, one::two::Nil) if one==two =>
        val links = OrganizedChangeableNode.emptyChangeMap[String].updated(Change.Unchanged, one.links)
        SiteNode(left, one, Change.Unchanged, links)
    }
    val updatedNodes: List[SiteNode] = grouped.collect{
      case (name, one::two::Nil) =>
        val states =  mergeSiteStates(left, one , two)
        val links = mergeLinks(one, two)
        new SiteNode(left, one, states, links, Change.Updated)
    }
    val removed: Set[Site] = left.sites.filterNot(s => sameNames.contains(s.name))
    val added: Set[Site] = right.sites.filterNot(s => sameNames.contains(s.name))
    val addedNodes: Set[SiteNode] = added.map{
      case a =>
        val links = OrganizedChangeableNode.emptyChangeMap[String].updated(Change.Added, a.links)
        SiteNode(left, a, Change.Added, links)
    }
    val removedNodes: Set[SiteNode] = removed.map{
      case r =>
        val links =  OrganizedChangeableNode.emptyChangeMap[String].updated(Change.Removed, r.links)
        SiteNode(left, r, Change.Removed, links)
    }
    Map(
      (Change.Removed , removedNodes),
      (Change.Added , addedNodes),
      (Change.Unchanged , unchangedNodes.toSet),
      (Change.Updated , updatedNodes.toSet)
    )
  }


  protected def mergeSiteStates(parent: Agent, left: KappaModel.Site, right: KappaModel.Site): Map[Change.Change, Set[StateNode]] = {
    val deleted: Set[State] = left.states.diff(right.states)
    val deletedNodes: Set[StateNode] = deleted.map(r => StateNode(left, r, Change.Removed))
    val added = right.states.diff(left.states)
    val addedNodes: Set[StateNode] = added.map(a => StateNode(left, a, Change.Added))
    val unchanged: Set[State] = left.states.intersect(right.states)
    val unchangedNodes: Set[StateNode] = unchanged.map(u => StateNode(left, u, Change.Unchanged))
    Map(
      (Change.Removed , deletedNodes),
      (Change.Added , addedNodes),
      (Change.Unchanged , unchangedNodes),
      (Change.Updated , Set.empty[StateNode])
    )
  }
  
  protected def onAgentUpdates(deleted: Set[Agent], added: Set[Agent], status: Change.Change) = {
      //dom.console.log(s"on added updates DELETED ${deleted} ADDED ${added} STATUS ${status}")
      val addedNodes = added.map(a => AgentNode(a, status))
      allAgentNodes() = allAgentNodes.now.filterNot(n => deleted.contains(n.agent) && n.status == status) ++ addedNodes
  }

  protected def onMergedUpdates(deleted: Set[AgentNode], added: Set[AgentNode]) = {
    //dom.console.log("MERGE UPDATE, DELETE = "+deleted + "  ADDED = "+added)
    allAgentNodes() = allAgentNodes.now.diff(deleted) ++ added
  }

  protected def getAllAgentNodes(node: AgentNode) = {
    //dom.console.log("children list ="+node.childrenList)
    val nds: List[KappaNode] = node :: node.childrenList ++ node.childrenList.flatMap(ch => ch.childrenList)
    nds.toSet
  }

  protected def getAllEdgesFrom(node: AgentNode): Set[KappaEdge] = {
    val edges = node.childEdgeList ++ node.childrenList.flatMap(ch => ch.childEdgeList)
    edges.toSet
  }

  protected def onAgentNodesUpdate(deleted: Set[AgentNode], added: Set[AgentNode]) = {
    val toDelete: Set[Node] = deleted.flatMap{ r => getAllAgentNodes(r)}
    //dom.console.log("TO REMOVE COUNT " + toDelete)
    val toAdd = added.flatMap( a => getAllAgentNodes(a))
    //dom.console.log("TO ADD COUNT " + toAdd)

    nodes() = nodes.now.filterNot(n => toDelete.contains(n)) ++ toAdd
    val addedEdges = added.flatMap(a => getAllEdgesFrom(a))
    edges() = edges.now.filterNot{ case e => toDelete.exists(a => a== e.from || a == e.to) } ++ addedEdges
  }


  def onNodesChanges(removed: Set[Node], added: Set[Node]): Unit = {
    //removed.foreach(r=> dom.console.log("REMOVED NODE "+r.view.label))
    //added.foreach(a=> dom.console.log("ADDED NODE "+a.view.label))

    removed.foreach{ case n =>
      ///dom.console.log("REMOVED IS "+n.view.label)
      n.view.clearChildren()
      viz.removeSprite(n.view.container)
      viz.removeObject(n.view.container)
    }
    added.foreach{
      case n =>
        //dom.console.log("ADDED IS "+n.view.label)
        viz.addSprite(n.view.container)
    }
    //if(added.nonEmpty || removed.nonEmpty) edges() = edges.now.filterNot(e => removed.contains(e.from) || removed.contains(e.to))
  }


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

  protected def createLinkEdges(mp: Map[String, Vector[(Change.Value, SiteNode)]]): Vector[Edge] = {
    mp.collect{
      //case (key, site::Nil) => //let us skip this for the sake of simplicity
      case (key, value)  if value.size ==2 =>
        val ((change, site1), (_, site2)) = (value(0), value(1))
        new KappaLinkEdge(key, site1, site2, change, linkLineByStatus(change, visualSettings.link.line))(createLinkView): Edge
      //case (key, other) => throw  new Exception(s"too many sites for the link $key ! Sights are: $other")
    }.toVector
  }

  protected def linkLineByStatus(status: Change.Change, line: LineParams) = status match {
    case Change.Added => line.copy(lineColor = Colors.green)
    case Change.Removed => line.copy(lineColor = Colors.red)
    case _ => line.copy(lineColor = Colors.blue  )
  }


  protected def onLinkChanged(removed: Map[String, Vector[(Change.Change, SiteNode)]],
                              added: Map[String, Vector[(Change.Change, SiteNode)]],
                              updated: Map[String, (Vector[(Change.Change, SiteNode)], Vector[(Change.Change, SiteNode)])]) = {

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

    onAgentNodesUpdate(Set.empty, allAgentNodes.now)
    allAgentNodes.updates.foreach{ case upd =>  onAgentNodesUpdate(upd.removed, upd.added) }

    //dom.console.log(s"UNCHANGED ON INIT + ${unchanged.now}")
    onAgentUpdates(Set.empty, unchanged.now, Change.Unchanged)
    unchanged.updates.foreach(upd => onAgentUpdates(upd.removed, upd.added, Change.Unchanged))

    //dom.console.log(s"REMOVED ON INIT + ${removed.now}")
    onAgentUpdates(Set.empty, removed.now, Change.Removed)
    removed.updates.foreach(upd => onAgentUpdates(upd.removed, upd.added, Change.Removed))

    //dom.console.log(s"ADDED ON INIT + ${added.now}")
    onAgentUpdates(Set.empty, added.now, Change.Added)
    added.updates.foreach(upd => onAgentUpdates(upd.removed, upd.added, Change.Added))

    //dom.console.log(s"MERGED ON INIT + ${merged.now}")
    onMergedUpdates(Set.empty, merged.now)
    merged.updates.foreach(upd=> onMergedUpdates(upd.removed, upd.added))

    layouts.now.foreach(_.start(viz.width, viz.height, viz.camera))
  }


  override def bindView() = {
    super.bindView()
    subscribeUpdates()
    viz.render()
  }

  lazy val layouts: Var[Vector[GraphLayout]] = Var(Vector(new RulesForceLayout(nodes, edges, ForceLayoutParams.default2D.mode, forces)))

}