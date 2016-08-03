package org.denigma.kappa.notebook.views.visual.rules


import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{Agent, Site}
import org.denigma.kappa.notebook.graph.Change.Change
import org.denigma.kappa.notebook.graph.layouts.LayoutInfo
import org.denigma.kappa.notebook.graph._
import org.scalajs.dom


object StateNode {
  def apply(parent: Site, state: KappaModel.State, status: Change)(implicit makeStateView: StateNode => KappaStateView) = new StateNode(parent, state, status)
}
class StateNode(val parent: Site, val state: KappaModel.State, val status: Change)
               (implicit val fun: StateNode => KappaStateView)
  extends ChangeableNode with MarkableNode
{
  val view = fun(this)

  lazy val layoutInfo: LayoutInfo = new LayoutInfo(0.3)
}


object SiteNode {
  def apply(parent: Agent, site: Site, status: Change)(
      implicit makeSiteNodeView: SiteNode => KappaSiteView,
      makeStateNodeView: StateNode => KappaStateView,
      getLineParams: (KappaNode, KappaNode) => LineParams
  )  = {
    val sts = site.states.map(s => new StateNode(site,s, status)(makeStateNodeView))
    val children = OrganizedChangeableNode.emptyChangeMap[StateNode].updated(status, sts)
    new SiteNode(parent, site, children, status)(makeSiteNodeView, getLineParams)
  }
}

class SiteNode(val parent: KappaModel.Agent, val site: KappaModel.Site, val children: Map[Change.Change,Set[StateNode]], val status: Change)
              (implicit val fun: SiteNode => KappaSiteView, getLineParams: (KappaNode, KappaNode) => LineParams)
  extends OrganizedChangeableNode with MarkableNode
{

  type ChildNode = StateNode
  type ChildEdge = KappaStateEdge

  lazy val view = fun(this)

  lazy val layoutInfo: LayoutInfo = new LayoutInfo(0.6)

  lazy val childEdges: Map[Change, Set[KappaStateEdge]] = children.mapValues{
    case st =>  st.map{ case ch=>new KappaStateEdge(this, ch, getLineParams(this, ch))} }
}

trait MarkableNode {
  def view: KappaView

  def markChanged() = {
    view.labelStrokeColor() = "violet"
  }

  def markDeleted() = {
    view.labelStrokeColor() = "red"
  }

  def markAdded() = {
    view.labelStrokeColor() = "green"
  }

  def markDefault() = {
    view.labelStrokeColor() = "blue"
  }

}
object AgentNode{
  //very ugly code, TODO: fix it
  def apply(agent: KappaModel.Agent, status: Change)(implicit
                                                     makeAgentNodeView: AgentNode => KappaAgentView,
                                                     makeSiteNodeView: SiteNode => KappaSiteView,
                                                     makeStateNodeView: StateNode => KappaStateView,
                                                     getLineParams: (KappaNode, KappaNode) => LineParams) ={
    val sts = agent.sites.map(s => SiteNode(agent, s, status)(makeSiteNodeView, makeStateNodeView, getLineParams))
    val children = OrganizedChangeableNode.emptyChangeMap[SiteNode].updated(status, sts)
    new AgentNode(agent,children, status)(makeAgentNodeView, getLineParams)
  }
}

//val data: Agent, val fontSize: Double, val padding: Double, val s: SVG
class AgentNode(val agent: KappaModel.Agent, val children: Map[Change.Change, Set[SiteNode]], val status: Change)
               (implicit val fun: AgentNode => KappaAgentView, getLineParams: (KappaNode, KappaNode) => LineParams)
  extends OrganizedChangeableNode with MarkableNode
{
  type ChildNode = SiteNode
  type ChildEdge = KappaSiteEdge

  lazy val layoutInfo: LayoutInfo = new LayoutInfo(1)

  lazy val view: KappaAgentView = fun(this)

  def childEdges: Map[Change.Change, Set[ChildEdge]] = children.mapValues{
    case set => set.map{ case s =>
          val lp =  getLineParams(this, s)
          new KappaSiteEdge(this, s, lp)
      }
  }
}
