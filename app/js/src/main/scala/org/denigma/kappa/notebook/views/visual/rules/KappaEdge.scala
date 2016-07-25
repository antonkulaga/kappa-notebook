package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.layouts.ForceEdge
import org.denigma.kappa.notebook.views.visual.rules.drawing.SvgBundle.all._
import org.denigma.kappa.notebook.views.visual.utils.LineParams
import rx._
import org.denigma.kappa.notebook.views.visual.rules.drawing.SvgBundle.all.attrs._
import org.denigma.threejs.{Side => _, _}
import org.scalajs.dom.svg.SVG

trait KappaEdge extends ForceEdge {

  override type FromNode <: KappaNode
  override type ToNode <: KappaNode

  def sourcePos: Vector3 = from.view.container.position
  def targetPos: Vector3 = to.view.container.position

}

trait ArrowEdge extends KappaEdge {


  def lineParams: LineParams

  def direction: Vector3 = new Vector3().subVectors(targetPos, sourcePos)

  def middle: Vector3 = new Vector3((sourcePos.x + targetPos.x) / 2,(sourcePos.y + targetPos.y) / 2, (sourcePos.z + targetPos.z) / 2)

  val arrow = new ArrowHelper(direction.normalize(), sourcePos, direction.length(), lineParams.lineColor, lineParams.headLength, lineParams.headWidth)

  protected def posArrow() = {
    arrow.position.set(sourcePos.x, sourcePos.y, sourcePos.z) // = sourcePos
    arrow.setDirection(direction.normalize())
    arrow.setLength(direction.length()-10, lineParams.headLength, lineParams.headWidth)
  }


  def update(): Unit = {
    posArrow()
  }


}

class KappaSiteEdge(val from: AgentNode, val to: SiteNode, val lineParams: LineParams = LineParams()) extends ArrowEdge{
  override type FromNode = AgentNode
  override type ToNode = SiteNode

  update()

}

class KappaStateEdge(val from: SiteNode, val to: StateNode, val lineParams: LineParams = LineParams()) extends ArrowEdge{
  override type FromNode = SiteNode
  override type ToNode = StateNode

  update()
}


class KappaLinkEdge(label: String, val from: SiteNode, val to: SiteNode, val lineParams: LineParams = LineParams())
      (implicit val fun: KappaLinkEdge => KappaLinkView)
  extends KappaEdge
{

  lazy val link = KappaModel.Link(from.parent.agent, to.parent.agent, from.site, to.site, label)

  val view = fun(this)

  type FromNode = SiteNode
  type ToNode = SiteNode

  lazy val fontSize = 14.0

  lazy val padding = 3.0

  type Data = KappaModel.Link

  def direction: Vector3 = new Vector3().subVectors(targetPos, sourcePos)

  protected def posArrow() = {
    arrow.position.set(sourcePos.x, sourcePos.y, sourcePos.z) // = sourcePos
    arrow.setDirection(direction.normalize())
    arrow.setLength(direction.length()-10, lineParams.headLength, lineParams.headWidth)
  }

  def middle: Vector3 = new Vector3((sourcePos.x + targetPos.x) / 2,(sourcePos.y + targetPos.y) / 2, (sourcePos.z + targetPos.z) / 2)

  protected def posSprite() = {
    val m = middle
    view.container.position.set(m.x, m.y, m.z)
  }

  import lineParams._
  val arrow = new ArrowHelper(direction.normalize(), sourcePos, direction.length(), lineColor, headLength, headWidth)

  //from.updateSideStroke(data.fromSide, lp.hexColor)
  //to.updateSideStroke(data.toSide, lp.hexColor)


  def update() = {
    posArrow()
    posSprite()
  }

  this.view.render()
  this.update()

}