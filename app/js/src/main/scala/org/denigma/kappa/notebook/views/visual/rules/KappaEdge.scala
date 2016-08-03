package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.graph.{ArrowEdge, Change, KappaEdge, LineParams}
import org.denigma.threejs.{Side => _, _}


class KappaSiteEdge(val from: AgentNode, val to: SiteNode, val lineParams: LineParams) extends ArrowEdge{
  override type FromNode = AgentNode
  override type ToNode = SiteNode

  update()

}

class KappaStateEdge(val from: SiteNode, val to: StateNode, val lineParams: LineParams) extends ArrowEdge{
  override type FromNode = SiteNode
  override type ToNode = StateNode

  update()
}


class KappaLinkEdge(label: String, val from: SiteNode, val to: SiteNode, val status: Change.Change, val lineParams: LineParams)
      (implicit val fun: KappaLinkEdge => KappaLinkView)
  extends ArrowEdge
{

  lazy val link = KappaModel.Link(from.parent, to.parent, from.site, to.site, label)

  val view: KappaLinkView = fun(this)

  type FromNode = SiteNode
  type ToNode = SiteNode

  lazy val fontSize = 14.0

  lazy val padding = 3.0

  type Data = KappaModel.Link

  protected def posSprite() = {
    val m = middle
    view.container.position.set(m.x, m.y, m.z)
  }


  override def update() = {
    posArrow()
    posSprite()
  }

  this.view.render()
  this.update()

}