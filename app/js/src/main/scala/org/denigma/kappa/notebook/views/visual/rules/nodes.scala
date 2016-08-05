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
  extends ChangeableNode
{
  val view = fun(this)

  lazy val layoutInfo: LayoutInfo = new LayoutInfo(0.3)
}


object SiteNode {


  def apply(parent: Agent, site: Site, status: Change, links: Map[Change.Change, Set[String]])
           (implicit makeSiteNodeView: SiteNode => KappaSiteView,
                    makeStateNodeView: StateNode => KappaStateView,
                    getLineParams: (KappaNode, KappaNode) => LineParams
           ): SiteNode =
  {
    val sts = site.states.map(s => new StateNode(site,s, status)(makeStateNodeView))
    val children = OrganizedChangeableNode.emptyChangeMap[StateNode].updated(status, sts)
    new SiteNode(parent, site, children, links,  status)(makeSiteNodeView, getLineParams)
  }

  /*
  def apply(parent: Agent, site: Site, status: Change)(
      implicit makeSiteNodeView: SiteNode => KappaSiteView,
      makeStateNodeView: StateNode => KappaStateView,
      getLineParams: (KappaNode, KappaNode) => LineParams
  ): SiteNode  = {
    val mp: Map[Change.Change, Set[String]] = OrganizedChangeableNode.emptyChangeMap[String]
    apply(parent, site, status, mp)(makeSiteNodeView, makeStateNodeView, getLineParams)
  }
  */
}

class SiteNode(val parent: KappaModel.Agent,
               val site: KappaModel.Site,
               val children: Map[Change.Change,Set[StateNode]],
               val links: Map[Change.Change, Set[String]],
               val status: Change)
              (implicit val fun: SiteNode => KappaSiteView, getLineParams: (KappaNode, KappaNode) => LineParams)
  extends OrganizedChangeableNode
{

  type ChildNode = StateNode
  type ChildEdge = KappaStateEdge

  lazy val view = fun(this)

  lazy val layoutInfo: LayoutInfo = new LayoutInfo(0.6)

  lazy val childEdges: Map[Change, Set[KappaStateEdge]] = children.mapValues{
    st =>  st.map{ ch=>
      val stat = (this.status, ch.status) match {
        case (Change.Added, _) => Change.Added
        case (Change.Removed, _) => Change.Removed
        case (_ , Change.Added) => Change.Added
        case (_ , Change.Removed) => Change.Removed
        case (_ , Change.Updated) => Change.Updated
        case _ => Change.Unchanged
      }
      new KappaStateEdge(this, ch, stat, getLineParams(this, ch))
    }
  }

  lazy val linkList: List[(String, Change)] = links.toList.flatMap{
    case (key, lks) => lks.map(l=> l->key)
  }
}

object AgentNode{
  //very ugly code, TODO: fix it
  def apply(agent: KappaModel.Agent, status: Change)(implicit
                                                     makeAgentNodeView: AgentNode => KappaAgentView,
                                                     makeSiteNodeView: SiteNode => KappaSiteView,
                                                     makeStateNodeView: StateNode => KappaStateView,
                                                     getLineParams: (KappaNode, KappaNode) => LineParams) ={
    val sts = agent.sites.map {
      s =>
        val links = OrganizedChangeableNode.emptyChangeMap[String].updated(status, s.links)
        SiteNode(agent, s, status, links)(makeSiteNodeView, makeStateNodeView, getLineParams)
    }
    val children = OrganizedChangeableNode.emptyChangeMap[SiteNode].updated(status, sts)
    new AgentNode(agent,children, status)(makeAgentNodeView, getLineParams)
  }
}

//val data: Agent, val fontSize: Double, val padding: Double, val s: SVG
class AgentNode(val agent: KappaModel.Agent, val children: Map[Change.Change, Set[SiteNode]], val status: Change)
               (implicit val fun: AgentNode => KappaAgentView, getLineParams: (KappaNode, KappaNode) => LineParams)
  extends OrganizedChangeableNode
{
  type ChildNode = SiteNode
  type ChildEdge = KappaSiteEdge

  lazy val layoutInfo: LayoutInfo = new LayoutInfo(1)

  lazy val view: KappaAgentView = fun(this)

  def childEdges: Map[Change.Change, Set[ChildEdge]] = children.mapValues {
    set => set.map { s =>
      val lp = getLineParams(this, s)
      val stat = (this.status, s.status) match {
        case (Change.Added, _) => Change.Added
        case (Change.Removed, _) => Change.Removed
        case (_ , Change.Added) => Change.Added
        case (_ , Change.Removed) => Change.Removed
        case (_ , Change.Updated) => Change.Updated
        case _ => Change.Unchanged
      }
      new KappaSiteEdge(this, s, stat, lp)
    }
  }
}
