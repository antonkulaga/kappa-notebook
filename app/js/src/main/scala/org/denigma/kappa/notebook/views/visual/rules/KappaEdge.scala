package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.kappa.model.{Change, KappaModel}
import org.denigma.kappa.notebook.graph._
import org.denigma.threejs.{Side => _, _}
import rx.Var


class KappaSiteEdge(val from: AgentNode, val to: SiteNode,val status: Change.Change, val lineParams: LineParams) extends ChangeableEdge {
  override type FromNode = AgentNode
  override type ToNode = SiteNode

  update()

}

class KappaStateEdge(val from: SiteNode, val to: StateNode, val status: Change.Change, val lineParams: LineParams) extends ChangeableEdge {
  override type FromNode = SiteNode
  override type ToNode = StateNode

  update()
}


class KappaLinkEdge(val from: SiteNode, val to: SiteNode, val status: Change.Change, val lineParams: LineParams)
  extends ChangeableEdge
{
  //lazy val link = KappaModel.Link(from.parent, to.parent, from.site, to.site, label)

  type FromNode = SiteNode
  type ToNode = SiteNode

  type Data = KappaModel.Link

  this.update()
}