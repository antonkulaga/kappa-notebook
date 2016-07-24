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

class KappaSightEdge(val from: AgentNode, val to: SightNode, val lineParams: LineParams = LineParams()) extends ArrowEdge{
  override type FromNode = AgentNode
  override type ToNode = SightNode

  update()

}

class KappaStateEdge(val from: SightNode, val to: StateNode, val lineParams: LineParams = LineParams()) extends ArrowEdge{
  override type FromNode = SightNode
  override type ToNode = StateNode

  update()
}


class KappaLinkEdge(val data: KappaModel.Link, val from: SightNode, val to: SightNode, val s: SVG, lp: LineParams = LineParams()) extends KappaEdge
{
  val view: KappaEdgeView = new KappaEdgeView(data.label, 16, 10, s)

  type FromNode = SightNode
  type ToNode = SightNode

  lazy val fontSize = 14.0

  lazy val padding = 3.0

  type Data = KappaModel.Link

  def direction: Vector3 = new Vector3().subVectors(targetPos, sourcePos)

  protected def posArrow() = {
    arrow.position.set(sourcePos.x, sourcePos.y, sourcePos.z) // = sourcePos
    arrow.setDirection(direction.normalize())
    arrow.setLength(direction.length()-10, lp.headLength, lp.headWidth)
  }

  def middle: Vector3 = new Vector3((sourcePos.x + targetPos.x) / 2,(sourcePos.y + targetPos.y) / 2, (sourcePos.z + targetPos.z) / 2)

  protected def posSprite() = {
    val m = middle
    view.container.position.set(m.x, m.y, m.z)
  }

  import lp._
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


/*

class SimpleNode(data: Var[String], view: NodeView[Var[String]]) extends VisualNode[Var[String], NodeView[Var[String]]](data, view)
{
  def id = data.now

  override def receive:PartialFunction[Any,Unit] = {


    case other => dom.console.log(s"unknown message $other")
    //nothing
  }

  def onMouseOver( event:MouseEvent ):Unit =   {
    send("mouseover")
  }


  def onMouseOut(sub:Subject)( event:MouseEvent ):Unit =   {
    send("mouseout")
  }

  view.sprite.element.addEventListener( "mouseover", (this.onMouseOver _).asInstanceOf[Function[Event,_ ]] )
  view.sprite.element.addEventListener( "mouseout", (this.onMouseOut _).asInstanceOf[Function[Event,_ ]] )


}

class SimpleEdge(from:SimpleNode,to:SimpleNode,data:Var[String],view:EdgeView[Var[String]]) extends VisualEdge[SimpleNode,Var[String],EdgeView[Var[String]]](from,to,data,view)
{
  def id = data.now

  override def receive:PartialFunction[Any,Unit] = {

    case "mouseover"=>
      //dom.console.log("mouse over works")
      this.view.sprite.element.className = this.view.sprite.element.className.replace("tiny","small")

    case "mouseout"=>
      dom.console.log("mouse out works")
      this.view.sprite.element.className = this.view.sprite.element.className.replace("small","tiny")


    case other => dom.console.log(s"unknown message $other")
    //nothing
  }
}


class NodeView[DataType](val data: DataType, val sprite: HtmlSprite, val colorName: String = Defs.colorName)
{

}

class EdgeView[EdgeDataType](val from: Object3D, val to: Object3D, val edgeData: EdgeDataType, val sprite: HtmlSprite, lp: LineParams = LineParams())
{

  def sourcePos: Vector3 = from.position
  def targetPos: Vector3 = to.position
  def middle = new Vector3((sourcePos.x+targetPos.x)/2,(sourcePos.y+targetPos.y)/2, (sourcePos.z+targetPos.z)/2)

  def direction = new Vector3().subVectors(targetPos, sourcePos)

  protected def posArrow() = {
    arrow.position = sourcePos
    arrow.setDirection(direction.normalize())
    arrow.setLength(direction.length()-10, lp.headLength, lp.headWidth)
  }

  protected def posSprite() = {
    val m = middle
    sprite.position.set(m.x,m.y,m.z)
  }

  import lp._
  val arrow =  new ArrowHelper(direction.normalize(), sourcePos, direction.length(), lineColor, headLength, headWidth)
  arrow.addEventListener("mouseover", this.onLineMouseOver _)
  arrow.addEventListener("mouseout", this.onLineMouseOver _)

  def onLineMouseOver(event:Any):Unit = {
    this.sprite.visible = true
    dom.console.log("onMouseOver")
  }

  def onLineMouseOut(event:Any):Unit = {
    this.sprite.visible = false
    dom.console.log("onMouseOut")
  }

  def update() = {
    posArrow()
    posSprite()
  }

  this.update()


}

*/