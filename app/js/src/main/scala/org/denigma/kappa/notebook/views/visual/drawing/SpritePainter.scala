package org.denigma.kappa.notebook.views.visual.drawing

/*
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{Agent, KappaNamedElement}
import org.denigma.threejs.Object3D
import org.denigma.threejs.extras.HtmlSprite
import org.scalajs.dom.raw._
import org.scalajs.dom.svg.{LinearGradient, SVG}

import scala.collection.immutable.::
import scalatags.JsDom
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.onclick
import SvgBundle.all._
import SvgBundle.all.attrs._

class BasicPainter(val s: SVG) {

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

  protected def inlineElements[T <: KappaNamedElement](sides: List[T], fontSize: Double, padding: Double) = {
    sides.foldLeft(List.empty[(T, Rectangle)], 0.0) {
      case ((list, cw), sd) =>
        val str = sd.name
        val box = getTextBox(str, fontSize)
        val nw = cw + box.width + padding * 3
        ((sd, box) :: list, nw)
    }
  }

  protected def alignHor[T <: KappaNamedElement](sides: List[(T, Rectangle)],
                                               padding: Double,
                                               parentWidth: Double): List[(T, Rectangle)] =
    sides match
    {

      case Nil => Nil

      case list =>
        val rev: List[(T, Rectangle)] = list.foldLeft(List.empty[(T, Rectangle)]) {
          case (Nil, (sd, box)) =>
            (sd, box.copy(x = 0))::Nil

          case ( (hs, hbox) :: tail, (sd, box)) =>
            val nx = hbox.right + padding * 2
            (sd, box.copy(x = nx))::(hs, hbox)::tail
        }
        val totalWidth = rev.head._2.right
        val start = Math.max((parentWidth - totalWidth) / 2, 0)
        //reversing back and shifting according to start value
        rev.foldLeft(List.empty[(T, Rectangle)]) {
          case ( acc, (sd, box)) =>
            val nx = box.x + start
            (sd, box.copy(x = nx))::acc
        }// -> Math.max(rev.head._2.right, parentWidth)
    }


  protected def alignVert[T](list: List[(T, Rectangle)], padding: Double, parentHeight: Double): List[(T, Rectangle)] = if(list.isEmpty) Nil else {
    val rev: List[(T, Rectangle)] = list.foldLeft(List.empty[(T, Rectangle)]) {
      case (Nil, (sd, box)) =>
        (sd, box.copy(y = 0))::Nil

      case ( (ws, hbox) :: tail, (sd, box)) =>
        val ny = hbox.bottom + padding * 2
        (sd, box.copy(y = ny))::(ws, hbox)::tail
    }
    val totalHeight = rev.head._2.bottom
    val start = Math.max((parentHeight - totalHeight) / 2, 0)
    //reversing back and shifting according to start value
    rev.foldLeft(List.empty[(T, Rectangle)]) {
      case ( acc, (sd, box)) =>
        val ny = box.y + start
        (sd, box.copy(y = ny))::acc
    }// -> Math.max(rev.head._2.right, parentWidth)
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


class SpritePainter(agentFontSize: Double, agentPadding: Double, s: SVG)  extends BasicPainter(agentFontSize, agentPadding, s){


  //val sideFontSize = agentFontSize / 1.6

  //val sidePadding = agentPadding / 1.6

  //val modificationPadding = sidePadding / 1.6


  protected lazy val agentGradient: TypedTag[LinearGradient] =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := "GradAgent",
      stop(offset := "0%", stopColor := "skyblue"),
      stop(offset := "50%", stopColor := "deepskyblue"),
      stop(offset := "100%", stopColor := "SteelBlue")
    )


  protected lazy val sideGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := "GradSide",
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "50%", stopColor := "skyblue"),
      stop(offset := "100%", stopColor := "deepskyblue")
  )

  protected lazy val modificationGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := "GradModif",
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )

  def drawAgent(agent: KappaModel.Agent): Object3D= {
    val b: Rectangle = getTextBox(agent.name, agentFontSize)
    drawAgent(agent, b)
  }


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



  def drawHorSides[T <: KappaNamedElement](parent: Object3D, tuples: List[(T, Rectangle)], y: Double): Unit = {
    for ((side, box) <- tuples) {
      val sideBox = box.copy(y = 0).withPadding(sidePadding, sidePadding)
      val label = drawLabel(side.name, sideBox, box, sideFontSize, "GradSide")
      val obj = new HtmlSprite(label.render)
      parent.add(obj)
      obj.position.set(sideBox.x, y, 0.0)
    }
  }


  def drawStates(side: Side) = side.states match {
    case Set.empty=> //nothing
    case states =>
      import KappaModel._

      val (boxes: List[(State, Rectangle)], w: Double) = states.foldLeft(List.empty[(State, Rectangle)], 0.0) {
        case ((list, cw), sd) =>
          val str = sd.name
          val box = getTextBox(str, sideFontSize)
          val nw = cw + box.width + sidePadding * 3
          ((sd, box) :: list, nw)
      }

  }

  def drawVertSides[T<:KappaNamedElement](parent: Object3D, tuples: List[(T, Rectangle)], y: Double) =  {
    for ((side, box) <- tuples) {
      val sideBox = box.copy(y = 0).withPadding(sidePadding, sidePadding)
      val label = drawLabel(side.name, sideBox, box, sideFontSize, "GradSide")
      val obj = new HtmlSprite(label.render)
      //println("obj position = "+obj.position.toArray())
      parent.add(obj)
      obj.position.set(sideBox.x, y, 0.0)
    }
  }

  protected def drawSprite(w: Double, h: Double, children: List[JsDom.Modifier]): TypedTag[SVG] = {
    val decs: JsDom.Modifier = defs(agentGradient, sideGradient, modificationGradient)
    val params = List(
      height := h,
      width := w,
      decs
    ) ++ children
    svg.apply(params: _*)
  }

  def drawLink(link: KappaModel.Link): TypedTag[SVG] = {
    val box: Rectangle = getTextBox(link.label, sideFontSize)
    val rect = Rectangle(box.width, box.height).withPadding(sidePadding, sidePadding)
    val linkLabel = drawLabel(link.label, rect, box, sideFontSize, "GradModif")
    drawSprite(rect.width, rect.height, List(linkLabel))
  }


}
*/