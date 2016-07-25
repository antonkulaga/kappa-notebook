package org.denigma.kappa.notebook.views.visual.rules


import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.layouts.{ForceNode, LayoutInfo}
import org.scalajs.dom.svg.SVG


trait KappaOrganizedNode extends KappaNode {
  type ChildNode <: KappaNode

  def children: List[ChildNode]

}

trait KappaNode extends ForceNode {

  val view: KappaView

  def layoutInfo: LayoutInfo

  def position = view.container.position
}

class SiteNode(val parent: AgentNode, val site: KappaModel.Site)(implicit val fun: SiteNode => KappaSiteView, val stateFun: StateNode => KappaStateView) extends KappaOrganizedNode{

  type ChildNode = StateNode

  val view = fun(this)

  lazy val layoutInfo: LayoutInfo = new LayoutInfo(0.6)

  lazy val children: List[ChildNode] = site.states.map(st=> new StateNode(this, st)(stateFun)).toList
}

class StateNode(val parent: SiteNode, val state: KappaModel.State)(implicit val fun: StateNode => KappaStateView) extends KappaNode {
  val view = fun(this)

  lazy val layoutInfo: LayoutInfo = new LayoutInfo(0.3)
}


//val data: Agent, val fontSize: Double, val padding: Double, val s: SVG
class AgentNode(val agent: KappaModel.Agent)
               (implicit
                val fun: AgentNode => KappaAgentView,
                val siteFun: SiteNode => KappaSiteView,
                val stateFun: StateNode => KappaStateView)
  extends KappaOrganizedNode
{
  type ChildNode = SiteNode

  lazy val layoutInfo: LayoutInfo = new LayoutInfo(1)

  val view = fun(this)

  def markChanged() = {
    view.labelStroke() = "violet"
  }

  def markDeleted() = {
    view.labelStroke() = "red"
  }

  def markAdded() = {
    view.labelStroke() = "green"
  }

  def markDefault() = {
    view.labelStroke() = "blue"
  }

  val children: List[SiteNode] = agent.sites.map(si=>new SiteNode(this, si)(siteFun, stateFun))

  /*
  def updateSideStroke(side: Side, color: String) = {
    children.collectFirst{
      case child if child.data ==side => child.labelStroke() = color
    }
  }

  def sidePosition(side: Side) = {
    val me = view.position
    children.collectFirst{
      case child if child.data == side => child.view.position
    }.map{
      case pos => new Vector3(me.x + pos.x, me.y + pos.y, me.z + pos.z)
    }.getOrElse{
      dom.console.error(s"cannot find  side($side)")
      me
    }
  }
  */
}
