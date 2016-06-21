package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.binding.extensions._
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{Agent, KappaNamedElement, _}
import org.denigma.kappa.notebook.views.visual.rules.drawing.SvgBundle.all._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import org.denigma.kappa.notebook.views.visual.rules.drawing.SvgBundle.all.attrs._
import org.denigma.kappa.notebook.views.visual.rules.drawing.{KappaPainter, Rectangle, SideBorder}
import org.denigma.threejs.extras.HtmlSprite
import org.denigma.threejs.{Side => _, _}
import org.scalajs.dom
import org.scalajs.dom.svg.{LinearGradient, SVG}

import scala.collection.immutable.::
import scalatags.JsDom.TypedTag

trait KappaView extends KappaPainter {

  type Data <: KappaNamedElement

  def data: Data
  def fontSize: Double
  def padding: Double
  protected def gradientName: String
  //protected def gradient: TypedTag[LinearGradient]
  def gradient:  Var[TypedTag[LinearGradient]]

  lazy val textBox = getTextBox(data.name, fontSize)

  lazy val labelBox = textBox.withPadding(padding, padding)

  protected val svg = Rx {
    labelStroke()
    val grad = gradient()
    val rect = Rectangle(textBox.width, textBox.height).withPadding(padding * 2, padding)
    val label = drawLabel(data.name, rect, textBox, fontSize, gradientName)
    drawSVG(rect.width, rect.height, List(gradient.now), List(label))
  }

  protected val sprite  = svg.map {
    case s => new HtmlSprite(s.render)
  }

  protected val spriteChange = sprite.zip
  spriteChange.onChange{
    case (old, n)=>
      if(old != n) {
        view.remove(old)
        view.add(n)
      }
  }


  lazy val view = new Object3D()

  def render(): Object3D = {
    clearChildren()
    view.add(sprite.now)
    view
  }

  def clearChildren() = {
    view.children.toList.foreach(view.remove)
  }

  render()
}

trait KappaParentView extends KappaView {
  type ChildView <: KappaView

  def children: List[ChildView]

  lazy val childrenShift = new Vector3(0, 0, 1)


  def setShift(pos: Vector3): Vector3 = {
    pos.set(pos.x + childrenShift.x, pos.y + childrenShift.y, pos.z + childrenShift.z)
  }


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
        val start = -(totalWidth / 2)//(Math.max(this.textBox.width - totalWidth, 0) - textBox.width) / 2
        //println(s"start(${start}) width${textBox.width} totalWidth${totalWidth}")
        //reversing back and shifting according to start value
        rev.foldLeft(List.empty[(ChildView, Rectangle)]) {
          case ( acc, (sd, box)) =>
            val nx = box.x + start + box.width / 2
            //println("BOX = "+ box.copy(x = nx)+" start = " + start)
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
    //val start = Math.max((textBox.height - totalHeight) / 2, 0)
    val start = (Math.max(this.textBox.height - totalHeight, 0) - textBox.height) / 2
    rev.foldLeft(List.empty[(ChildView, Rectangle)]) {
      case ( acc, (sd, box)) =>
        val ny = box.y + start + box.height / 2
        (sd, box.copy(y = ny))::acc
    }// -> Math.max(rev.head._2.right, parentWidth)
  }

  def drawChildren(updateChildren: Boolean): Unit = {

    //val (boxes: List[(T, Rectangle)], w: Double) = inlineSides(sides, sideFontSize, sidePadding * 3)
    val (boxes: List[(ChildView, Rectangle)], w: Double) = childrenLine()
    val border = SideBorder.extract(textBox.width, boxes.reverse, w, padding)
    //val (top, bottom) = (alignHor(bottom.t), alignHor())
    val left = alignVert(border.left)
    val top = alignHor(border.top)
    val bottom = alignHor(border.bottom)
    val right = alignVert(border.right)

    drawVertSides(left, -labelBox.width / 2, updateChildren, true)
    drawHorSides(top, labelBox.height - padding, updateChildren)
    drawHorSides(bottom, -labelBox.height + padding, updateChildren)
    drawVertSides(right, labelBox.width / 2, updateChildren, false)
  }

  def drawHorSides(tuples: List[(ChildView, Rectangle)], y: Double, updateChildren: Boolean): Unit = {
    for ((child, box) <- tuples) {
      val obj = child.render()
      setShift(obj.position.set(box.x, y, 0.0))
      view.add(obj)
    }
  }

  def drawVertSides(tuples: List[(ChildView, Rectangle)], x: Double, updateChildren: Boolean, left: Boolean): Unit = {
    for ((child, box) <- tuples) {
      val obj = child.render()
      val shift = if(left) - box.width / 2 else box.width / 2
      setShift(obj.position.set(x + shift, box.y, 0.0))
      view.add(obj)
    }
  }

  override def render(): Object3D = {
    super.render()
    drawChildren(true)
    view
    //container.children.foreach(c=>scene.remove(c))
  }

  override def clearChildren() = {
    view.children.toList.foreach(view.remove)
    children.foreach(_.clearChildren())
  }
}

class KappaNodeView(val data: Agent, val fontSize: Double, val padding: Double, val s: SVG) extends KappaParentView
{

  type Data = Agent

  type ChildView = KappaSideView

  override def gradientName: String = "GradAgent"

  protected lazy val defaultGradient: TypedTag[LinearGradient] =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "skyblue"),
      stop(offset := "50%", stopColor := "deepskyblue"),
      stop(offset := "100%", stopColor := "SteelBlue")
    )

  lazy val gradient = Var(defaultGradient)

  lazy val children: List[KappaSideView] = data.sides.map(side=> new KappaSideView(side, fontSize / 1.6, padding / 2, s))

}

class KappaSideView(val data: Side, val fontSize: Double, val padding: Double, val s: SVG) extends  KappaParentView
{

  type Data = Side

  type ChildView = KappaStateView


  override def gradientName: String =  "GradSide"

  protected lazy val defaultGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )

  lazy val gradient = Var(defaultGradient)

  lazy val children: List[ChildView] = data.states.toList.map(state=> new KappaStateView(state, fontSize / 1.6, padding / 1.6, s))

}

class KappaStateView(val data: State, val fontSize: Double, val padding: Double, val s: SVG) extends KappaView
{

  lazy val gradientName = "GradModif"

  protected lazy val defaultGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "ivory")
    )

  lazy val gradient = Var(defaultGradient)

  type Data = State

}


object KappaNodeView {
  def apply(agent: Agent): Unit = {

  }
}
//val data: Agent, val fontSize: Double, val padding: Double, val s: SVG
class AgentNode(agent: KappaModel.Agent, s: SVG) extends KappaNodeView(agent, 24.0, 10, s ){

  val layoutInfo = new LayoutInfo()

  def markChanged() = {
    labelStroke() = "violet"
  }

  def markDeleted() = {
    labelStroke() = "red"
  }

  def markAdded() = {
    labelStroke() = "green"
  }

  def markDefault() = {
    labelStroke() = "blue"
  }

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

}
