package org.denigma.kappa.notebook.views.visual

import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{Agent, KappaNamedElement, _}
import org.denigma.kappa.notebook.views.visual.drawing.SvgBundle.all._
import org.denigma.kappa.notebook.views.visual.drawing.SvgBundle.all.attrs._
import org.denigma.kappa.notebook.views.visual.drawing.{Rectangle, SideBorder, SvgBundle}
import org.denigma.threejs.extras.HtmlSprite
import org.denigma.threejs.{Side => _, _}
import org.scalajs.dom
import org.scalajs.dom.raw.{SVGElement, SVGLocatable}
import org.scalajs.dom.svg.{LinearGradient, SVG}

import scala.collection.immutable.::
import scalatags.JsDom
import scalatags.JsDom.TypedTag

trait KappaPainter {

  def s: SVG

  type Locatable = SVGElement with SVGLocatable

  def getTextBox(str: String, fSize: Double): Rectangle = {
    val svg = text(str, fontSize := fSize)
    getBox(svg.render)
  }

  def getBox(e: Locatable): Rectangle = {
    s.appendChild(e)
    val box = e.getBBox()
    s.removeChild(e)
    box
  }

  def drawSVG(w: Double, h: Double, definitions: List[JsDom.Modifier], children: List[JsDom.Modifier]): TypedTag[SVG] = {
    val decs = defs(definitions:_*)
    val params = List(
      height := h,
      width := w,
      decs
    ) ++ children
    svg.apply(params: _*)
  }


  def drawLabel(str: String, rectangle: Rectangle, textBox: Rectangle,
                fSize: Double, grad: String): TypedTag[SVG] = {
    val st = 1
    val r = rect(
      stroke := "blue",
      fill := s"url(#${grad})",
      strokeWidth := st,
      height := rectangle.height,
      width := rectangle.width,
      rx := 50, ry := 50
    )
    val startX = (rectangle.width - textBox.width) / 2
    val startY = (rectangle.height - textBox.height) / 2 + textBox.height
    val txt = text(str, fontSize := fSize, x := startX, y := startY)
    import scalatags.JsDom.implicits._

    svg(
      height := rectangle.height + st,
      width := rectangle.width + st,
      x := rectangle.x,
      y := rectangle.y,
      //onclick := { ev: MouseEvent=> println("hello")},
      r, txt
    )
  }

}

trait KappaView extends KappaPainter {

  type Data <: KappaNamedElement

  def data: Data
  def fontSize: Double
  def padding: Double
  protected def gradient: TypedTag[LinearGradient]
  protected def gradientName: String

  lazy val textBox = getTextBox(data.name, fontSize)

  def svg(): TypedTag[SVG] = {
    val rect = Rectangle(textBox.width, textBox.height).withPadding(padding * 2, padding)
    val agentLabel = drawLabel(data.name, rect, textBox, fontSize, gradientName)
    val result = drawSVG(rect.width, rect.height, List(gradient), List(agentLabel))
    result
  }

  lazy val container = new Object3D()

  def render(): Object3D = {
    clearChildren()
    val elem = svg().render
    val box = elem.getBBox()
    val sprite = new HtmlSprite(elem)
    clearChildren()
    container.add(sprite)
    container
    //container.children.foreach(c=>scene.remove(c))
  }

  protected def clearChildren() = {
    container.children.toList.foreach(container.remove)
  }
}

trait KappaParentView extends KappaView {
  type ChildView <: KappaView

  def children: List[ChildView]

  protected def childrenLine(): (List[(ChildView, Rectangle)], Double) = {
    children.foldLeft(List.empty[(ChildView, Rectangle)], 0.0) {
      case ((list, cw), child) =>
        val box = child.textBox
        val nw = cw + box.width + child.padding * 3
        ((child, box) :: list, nw)
    }
  }

  protected def alignHor(elements: List[(ChildView, Rectangle)]): List[(ChildView, Rectangle)] =
    elements match
    {

      case Nil => Nil

      case list =>
        val rev= list.foldLeft(List.empty[(ChildView, Rectangle)]) {
          case (Nil, (child, box)) =>
            (child, box.copy(x = 0))::Nil

          case ((prev, prevBox) :: tail, (child, box)) =>
            val nx = prevBox.right + child.padding * 2
            (child, box.copy(x = nx))::(prev, prevBox)::tail
        }
        val totalWidth = rev.head._2.right
        val start = Math.max((this.textBox.width - totalWidth) / 2, 0)
        println(s"start(${start}) width${textBox.width} totalWidth${totalWidth}")
        //reversing back and shifting according to start value
        rev.foldLeft(List.empty[(ChildView, Rectangle)]) {
          case ( acc, (sd, box)) =>
            val nx = box.x + start
            (sd, box.copy(x = nx))::acc
        }// -> Math.max(rev.head._2.right, parentWidth)
    }

  protected def alignVert(list: List[(ChildView, Rectangle)]): List[(ChildView, Rectangle)] = if(list.isEmpty) Nil else {
    val rev: List[(ChildView, Rectangle)] = list.foldLeft(List.empty[(ChildView, Rectangle)]) {
      case (Nil, (sd, box)) =>
        (sd, box.copy(y = 0))::Nil

      case ( (prev, prevBox) :: tail, (child, box)) =>
        val ny = prevBox.bottom + padding * 2
        (child, box.copy(y = ny))::(prev, prevBox)::tail
    }
    val totalHeight = rev.head._2.bottom
    val start = Math.max((textBox.height - totalHeight) / 2, 0)
    //reversing back and shifting according to start value
    rev.foldLeft(List.empty[(ChildView, Rectangle)]) {
      case ( acc, (sd, box)) =>
        val ny = box.y + start
        (sd, box.copy(y = ny))::acc
    }// -> Math.max(rev.head._2.right, parentWidth)
  }

  def drawChildren(updateChildren: Boolean): Unit = {

    //val (boxes: List[(T, Rectangle)], w: Double) = inlineSides(sides, sideFontSize, sidePadding * 3)
    val (boxes: List[(ChildView, Rectangle)], w: Double) = childrenLine()
    val border = SideBorder.extract(textBox.width, boxes.reverse, w, padding)
    //val (top, bottom) = (alignHor(bottom.t), alignHor())
    val top = alignHor(border.top)
    val bottom = alignHor(border.top)
    val left = alignVert(border.left)
    val right = alignVert(border.right)

    drawHorSides(top, -textBox.height, updateChildren)
    drawHorSides(bottom, textBox.height, updateChildren)
    drawVertSides(left, -textBox.left, updateChildren)
    drawVertSides(right, textBox.right, updateChildren)
  }

  def drawHorSides(tuples: List[(ChildView, Rectangle)], y: Double, updateChildren: Boolean): Unit = {
    for ((child, box) <- tuples) {
      val obj = child.render()
      obj.position.set(box.x, y, 0.0)
      container.add(obj)
    }
  }

  def drawVertSides(tuples: List[(ChildView, Rectangle)], x: Double, updateChildren: Boolean): Unit = {
    for ((child, box) <- tuples) {
      val obj = child.render()
      obj.position.set(x, box.y, 0.0)
      container.add(obj)
    }
  }

  override def render(): Object3D = {
    super.render()
    drawChildren(true)
    container
    //container.children.foreach(c=>scene.remove(c))
  }
}

class KappaNodeView(val data: Agent, val fontSize: Double, val padding: Double, val s: SVG) extends KappaParentView
{

  type Data = Agent

  type ChildView = KappaSideView

  override def gradientName: String = "GradAgent"

  override protected lazy val gradient: TypedTag[LinearGradient] =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "skyblue"),
      stop(offset := "50%", stopColor := "deepskyblue"),
      stop(offset := "100%", stopColor := "SteelBlue")
    )


  /*
  def drawAgent(agent: Agent, box: Rectangle): Object3D = {

    val rect = Rectangle(box.width, box.height).withPadding(agentPadding * 2, agentPadding)
    val agentLabel = drawLabel(agent.name, rect, box, agentFontSize, "GradAgent")
    val sp = drawSprite(rect.width, rect.height, List(agentLabel))
    val parent = new Object3D()
    parent.add(new HtmlSprite(sp.render))
    agent.sides match {
      case Nil =>
        parent

      case sides =>
        drawChildren(box, parent, sides)

        parent
    }
  }

  def drawChildren[T <: KappaNamedElement](box: Rectangle, parent: Object3D, sides: List[T]): Unit = {

    val (boxes: List[(T, Rectangle)], w: Double) = inlineSides(sides, sideFontSize, sidePadding * 3)

    val border = SideBorder.extract(box.width, boxes.reverse, w, sidePadding)
    //val (top, bottom) = (alignHor(bottom.t), alignHor())
    val top = alignHor(border.top, sidePadding, box.width)
    val bottom = alignHor(border.top, sidePadding, box.width)
    val left = alignVert(border.left, sidePadding, box.height)
    val right = alignVert(border.right, sidePadding, box.height)

    drawHorSides(parent, top, box.width)
    drawHorSides(parent, bottom, -box.width)
    drawVertSides(parent, left, -box.height)
    drawVertSides(parent, right, box.height)
  }
  */


  protected def updateChildren(rerender: Boolean): Unit = {

  }

  lazy val children: List[KappaSideView] = data.sides.map(side=> new KappaSideView(side, fontSize / 1.6, padding / 1.6, s))

}

class KappaSideView(val data: Side, val fontSize: Double, val padding: Double, val s: SVG) extends  KappaParentView
{

  type Data = Side

  type ChildView = KappaStateView


  override def gradientName: String =  "GradSide"

  protected lazy val gradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "50%", stopColor := "skyblue"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )

  protected def updateChildren(rerender: Boolean): Unit = {

  }


  lazy val children: List[ChildView] = data.states.toList.map(state=> new KappaStateView(state, fontSize / 1.6, padding / 1.6, s))

}

class KappaStateView(val data: State, val fontSize: Double, val padding: Double, val s: SVG) extends KappaView
{

  val gradientName = "GradModif"

  protected lazy val gradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName
      ,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )

  type Data = State

  protected def updateChildren(rerender: Boolean): Unit = {

  }

}


object KappaNodeView {
  def apply(agent: Agent): Unit = {

  }
}
//val data: Agent, val fontSize: Double, val padding: Double, val s: SVG
class KappaNode(agent: KappaModel.Agent, s: SVG) extends KappaNodeView(agent, 24.0, 10, s ){

  val layoutInfo = new LayoutInfo()

}

class KappaEdge(val data: KappaModel.Link, val from: KappaNode, val to: KappaNode, val s: SVG, lp: LineParams = LineParams()) extends KappaView{

  lazy val fontSize = 20.0

  lazy val padding = 5.0

  type Data = KappaModel.Link

  val gradientName = "GradModif"

  protected lazy val gradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName
      ,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )


  def sourcePos: Vector3 = from.container.position
  def targetPos: Vector3 = to.container.position
  def middle = new Vector3((sourcePos.x + targetPos.x) / 2,(sourcePos.y + targetPos.y) / 2, (sourcePos.z + targetPos.z) / 2)

  def direction = new Vector3().subVectors(targetPos, sourcePos)

  protected def posArrow() = {
    arrow.position.set(sourcePos.x, sourcePos.y, sourcePos.z) // = sourcePos
    arrow.setDirection(direction.normalize())
    arrow.setLength(direction.length()-10, lp.headLength, lp.headWidth)
  }

  protected def posSprite() = {
    val m = middle
    container.position.set(m.x, m.y, m.z)
  }

  import lp._
  val arrow = new ArrowHelper(direction.normalize(), sourcePos, direction.length(), lineColor, headLength, headWidth)
  arrow.addEventListener("mouseover", this.onLineMouseOver _)
  arrow.addEventListener("mouseout", this.onLineMouseOver _)

  def onLineMouseOver(event: Any): Unit = {
    container.visible = true
    dom.console.log("onMouseOver")
  }

  def onLineMouseOut(event: Any): Unit = {
    container.visible = false
    dom.console.log("onMouseOut")
  }

  def update() = {
    posArrow()
    posSprite()
  }

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