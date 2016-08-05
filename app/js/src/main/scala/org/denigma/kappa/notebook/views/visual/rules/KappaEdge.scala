package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.kappa.model.KappaModel
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


class KappaLinkEdge(label: String, val from: SiteNode, val to: SiteNode, val status: Change.Change, val lineParams: LineParams, div: Double)
      (implicit val fun: KappaLinkEdge => KappaLinkView)
  extends ChangeableEdge
{

  val divider = Var(div)

  override def middleDivider: Double = divider.now

  override def opacity_=(value: Double): Unit = {
    arrow.cone.material.opacity = value
    arrow.line.material.opacity = value
    view.opacity = value
  }

  lazy val link = KappaModel.Link(from.parent, to.parent, from.site, to.site, label)

  val view: KappaLinkView = fun(this)

  type FromNode = SiteNode
  type ToNode = SiteNode

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