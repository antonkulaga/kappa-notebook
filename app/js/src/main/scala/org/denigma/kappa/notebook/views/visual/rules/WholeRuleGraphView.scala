package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.binding.extensions._
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{Agent, Site, State}
import org.denigma.kappa.notebook.graph.Change.Change
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.layouts.{ForceLayoutParams, GraphLayout}
import org.denigma.kappa.notebook.parsers.GraphUpdate
import org.denigma.kappa.notebook.views.visual.ShowParameters
import org.scalajs.dom
import org.scalajs.dom.raw.{ClientRect, Element, HTMLElement}
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.Predef.{Map, Set}
import scala.Vector
import scala.collection.immutable._

class WholeRuleGraphView(val elem: Element,
                         val update: Rx[GraphUpdate],
                         val showState: Rx[ShowParameters.ShowParameters],
                         val containerName: String,
                         val visualSettings: RulesVisualSettings
                        ) extends RuleGraphWithForces
{

  lazy val unchanged: Rx[Set[Agent]] = update.map(u=>u.unchangedAgents)

  lazy val updated: Rx[Set[(Agent, Agent)]] = update.map(u => u.updatedAgents)

  lazy val removed: Rx[Set[Agent]] = update.map(u => u.removedAgents)

  lazy val added: Rx[Set[Agent]] =  update.map(u => u.addedAgents)

  lazy val size: (Double, Double) = elem.getBoundingClientRect() match {
    case rect: ClientRect if rect.width < 100 || rect.height < 100 => (400.0, 400.0)
    case rect: ClientRect => (rect.width, rect.height)
  }
  val width: Var[Double] = Var(size._1)

  val height: Var[Double] = Var(size._2)

  lazy val nodes = Var(Vector.empty[Node])

  lazy val edges = Var(Vector.empty[Edge])

  lazy val container: HTMLElement = sq.byId(containerName).get

  val allAgentNodes: Var[Set[AgentNode]] = Var(Set.empty[AgentNode])

  lazy val siteNodes: Rx[Vector[SiteNode]] = nodes.map(nds => nds.collect{case node: SiteNode => node})

  lazy val merged = updated.map{ up => up.map{  case (one, two) => mergeNodes(one, two) }}

  lazy val links: Rx[Map[(String, Change), Vector[SiteNode]]] = siteNodes.map{ sNodes =>
    //sNodes.foreach(s=>dom.console.log(s" SITE ${s.site} LINKS = "+ s.links))
    val lns: Map[(String, Change), Vector[SiteNode]] = sNodes
      .flatMap(snode => snode.linkList.map(ls => (ls , snode)))
      .groupBy{ case ((link, change), nds) => (link, change) }
      .mapValues(vector => vector.map { case ((site, change), node) => node })
    lns
  }


  val viz = new Visualizer(container,
    width,
    height,
    layouts,
    800,
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
    val all = left.sites.toList ++ right.sites.toList
    val grouped = all.groupBy(s=>s.name).toList
    val unchangedNodes: List[SiteNode] = grouped.collect{
      case (name, one::two::tail) if one==two =>
        if(tail.nonEmpty) dom.console.error("tail is not empty, sites are: " + one::two::tail)
        val links = OrganizedChangeableNode.emptyChangeMap[String].updated(Change.Unchanged, one.links)
        SiteNode(left, one, Change.Unchanged, links)
    }
    val updatedNodes: List[SiteNode] = grouped.collect{
      case (name, one::two::tail) if one!=two  =>
        if(tail.nonEmpty) dom.console.error("tail is not empty, sites are: " + one::two::tail)     
        val states =  mergeSiteStates(one, two)
        val links = mergeLinks(one, two)
        new SiteNode(left, one, states, links, Change.Updated)
    }
    val removed: Set[Site] = left.sites.filterNot(s => sameNames.contains(s.name))
    val added: Set[Site] = right.sites.filterNot(s => sameNames.contains(s.name))
    val addedNodes: Set[SiteNode] = added.map{ a =>
        val links = OrganizedChangeableNode.emptyChangeMap[String].updated(Change.Added, a.links)
        SiteNode(left, a, Change.Added, links)
    }
    val removedNodes: Set[SiteNode] = removed.map{ r =>
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

  protected def getAllEdgesFrom(node: AgentNode): Set[Edge] = {
    val edges = node.childEdgeList ++ node.childrenList.flatMap(ch => ch.childEdgeList)
    edges.toSet
  }

  protected def onAgentNodesUpdate(deleted: Set[AgentNode], added: Set[AgentNode]) = {
    val toDelete: Set[Node] = deleted.flatMap{ r => getAllAgentNodes(r)}
    //dom.console.log("TO REMOVE COUNT " + toDelete)
    val toAdd: Set[Node] = added.flatMap(a => getAllAgentNodes(a))
    //dom.console.log("TO ADD COUNT " + toAdd)

    nodes() = nodes.now.filterNot(n => toDelete.contains(n)) ++ toAdd
    val addedEdges = added.flatMap(a => getAllEdgesFrom(a))
    edges() = edges.now.filterNot{ e => toDelete.exists(a => a== e.from || a == e.to) } ++ addedEdges
  }

import org.denigma.kappa.notebook.views.visual.ShowParameters.ShowParameters

  def onNodesChanges(removed: Set[Node], added: Set[Node]): Unit = {
    //removed.foreach(r=> dom.console.log("REMOVED NODE "+r.view.label))
    //added.foreach(a=> dom.console.log("ADDED NODE "+a.view.label))

    removed.foreach{ n =>
      ///dom.console.log("REMOVED IS "+n.view.label)
      n.view.clearChildren()
      viz.removeSprite(n.view.container)
      viz.removeObject(n.view.container)
    }
    added.foreach{
      n =>
        //dom.console.log("ADDED IS "+n.view.label)
        updateNodeVisibility(n, showState.now)
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
      case arr: Edge => viz.removeObject(arr.arrow)
      case _ => dom.console.log("weird edge")
    }
    for(a <- added) a match {
      case link: KappaLinkEdge =>
        viz.addSprite(link.view.container)
        viz.addObject(link.arrow)
      case arr: Edge => viz.addObject(arr.arrow)
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
        new KappaLinkEdge(name, site1, site2, change, lineParams, 2.0)(createLinkView): Edge
      //case (key, other) => throw  new Exception(s"too many sites for the link $key ! Sights are: $other")
    }.toVector
  }

  protected def linkLineByStatus(status: Change.Change, line: LineParams) = status match {
    case Change.Added => line.copy(lineColor = Colors.green)
    case Change.Removed => line.copy(lineColor = Colors.red)
    case _ => line.copy(lineColor = Colors.blue  )
  }

  protected def onLinkChanged(deleted: Map[(String, Change.Change), Vector[SiteNode]],
                              added: Map[(String, Change.Change), Vector[SiteNode]],
                              updated: Map[(String, Change.Change), (Vector[SiteNode], Vector[SiteNode])]) = {
    val delValues: Set[SiteNode] = deleted.values.flatMap(v=>v).toSet
    val (prev: Iterable[((String, Change.Change), Vector[SiteNode])],
    curr: Iterable[((String, Change.Change), Vector[SiteNode])]) = updated.unzip[((String, Change.Change), Vector[SiteNode]), ((String, Change.Change), Vector[SiteNode])]{
      case (key, (v1, v2)) => ((key, v1), (key, v2))
    }
    val old: Set[SiteNode] =  prev.toMap.values.flatMap(v=>v).toSet
    val cr: Map[(String, Change.Change), Vector[SiteNode]] = curr.toMap
    edges() = edges.now.filterNot{
      case edge: KappaLinkEdge => delValues.contains(edge.from) || delValues.contains(edge.to) || old.contains(edge.from) || old.contains(edge.to) //|| updated.contains(edge.link.label)
      case other => false
    } ++ createLinkEdges(added) ++ createLinkEdges(cr)
  }


  protected def subscribeUpdates() = {
    showState.foreach{
      sh =>
        nodes.now.foreach(n => updateNodeVisibility(n, sh))
        edges.now.foreach(e => updateEdgeVisibility(e, sh))
    }

    nodes.removedInserted.foreach{
      case (r, a) => onNodesChanges(r.toSet, a.toSet)
    }
    edges.removedInserted.foreach{
      case (r, a) => onEdgesChanges(r.toSet, a.toSet)
    }

    links.updates.foreach{
      upd => onLinkChanged(upd.removed, upd.added, upd.updated)
    }

    onAgentNodesUpdate(Set.empty, allAgentNodes.now)
    allAgentNodes.updates.foreach{ upd =>  onAgentNodesUpdate(upd.removed, upd.added) }

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

  //bad code, TODO: fix it
  protected def updateEdgeVisibility(edge: KappaEdge, show: ShowParameters) = (edge, show) match {
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
  protected def updateNodeVisibility(node: Node, show: ShowParameters) = (node, show) match {
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

  lazy val layouts: Var[Vector[GraphLayout]] = Var(Vector(new RulesForceLayout(nodes, edges, ForceLayoutParams.default3D.mode, forces)))

}