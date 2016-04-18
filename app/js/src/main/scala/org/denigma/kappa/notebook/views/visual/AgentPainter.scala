package org.denigma.kappa.notebook.views.visual


import org.denigma.binding.extensions.sq
import org.denigma.binding.views.BindableView
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.Side
import org.denigma.threejs.{Object3D, Vector3}
import org.denigma.threejs.extensions.Container3D
import org.denigma.threejs.extensions.controls.JumpCameraControls
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw._
import org.scalajs.dom.svg.{Defs, SVG}
import rx._

import scala.collection.immutable.::
import scalatags.JsDom
import scalatags.JsDom.{TypedTag, all}
import scalatags.JsDom.svgAttrs
import svgAttrs._
import scalatags.JsDom.svgTags._
import scalatags.JsDom.implicits._

object Rectangle {

  implicit def fromSVG(rect: SVGRect): Rectangle = Rectangle(rect.x, rect.y, rect.width, rect.height)

  def fromCorners(left: Double, top: Double, right: Double, bottom: Double): Rectangle = {
    require( right >= left && bottom >= top, s"rectangle width and height should always be positive")

    Rectangle(left, top, right - left, bottom - top)
  }

  def apply(width: Double, height: Double): Rectangle = Rectangle(0.0, 0.0, width, height)


}

case class Rectangle(x: Double, y: Double, width: Double, height: Double) {
  require( width >= 0.0 && height >= 0.0, s"rectangle width and height should always be positive")

  def left = x
  def top = y
  lazy val right = x + width
  lazy val bottom = y + height

  def merge(rect: Rectangle) = {
    Rectangle.fromCorners(
      Math.min(rect.left, left),
      Math.min(rect.top, top),
      Math.max(rect.right, right),
      Math.max(rect.bottom, bottom))
  }

  lazy val ox = x + width / 2.0
  lazy val oy = x + height / 2.0

  def centerHor(rect: Rectangle) = copy(x = rect.ox - 0.5 * width)
  def centerVert(rect: Rectangle) = copy(y = rect.oy - 0.5 * height)

  def withPadding(horPadding: Double, verPadding: Double): Rectangle = copy(height = height + verPadding * 2, width = width + horPadding *2)
}

object SideBorder {

  private def div2(sides: List[(Side, Rectangle)]) = Math.round(sides.size / 2.0).toInt

  private def splitByWidth(sides: List[(Side, Rectangle)], w: Double, padding: Double): (List[(Side, Rectangle)], List[(Side, Rectangle)], Double) =
  {
    val (t, r, s) = sides.foldLeft((List.empty[(Side, Rectangle)], List.empty[(Side, Rectangle)], 0.0)) {
      case ( (taken, rest, sm), (side, re)) =>
        if( (sm + padding * 2) > w) (taken, (side, re)::rest, sm)
        else {
          val curSm = sm + padding * 2 + re.width
          ((side, re)::taken, rest, sm)
        }
    }
    (t.reverse, r.reverse, s) //in foldLeft we collected them in reverse way

  }

  def extract(parentWidth: Double, sides: List[(Side, Rectangle)], totalWidth: Double, padding: Double) = totalWidth match {

    case w if w <= parentWidth * 2 =>
      val (top, bottom, sm) = splitByWidth(sides, parentWidth, padding)
      SideBorder(top, bottom)

    case w =>
      val (top, list, _) = splitByWidth(sides, parentWidth, padding)
      //println(splitByWidth(sides, parentWidth, padding))
      list match {

        case Nil =>  SideBorder(top, Nil) //if right is left better to add it to the bottom

        case right::Nil => SideBorder(top, List(right))

        case right::smth::Nil => SideBorder(top, List(right), List(smth))

        case right::other =>

          splitByWidth(other, parentWidth, padding) match {
              case (bottom, Nil, _) => SideBorder(top, List(right), bottom)
              case (bottom, left::Nil, _) => SideBorder(top, List(right), bottom, List(left))
              case (bottom, left, sm) =>
                dom.console.error(s"an agent has so many sides that I cannot visualize it!" +
                  s"sides are [${sides.map(_._1).mkString(" ")}]")
                val (btm, last) = left.splitAt(left.size -1)
                SideBorder(top, List(right), bottom++btm, last)
          }
      }

  }
}

case class SideBorder(top: List[(Side, Rectangle)], bottom: List[(Side, Rectangle)], left: List[(Side, Rectangle)] = List.empty, right:List[(Side, Rectangle)] = List.empty)
{

}

class AgentPainter(agentFontSize: Double, agentPadding: Double, s: SVG) {

  val sideFontSize = agentFontSize / 1.6

  val sidePadding = agentPadding / 1.6

  type Locatable = SVGElement with SVGLocatable

  protected def getTextBox(str: String, fSize: Double): Rectangle = {
    getBox(text(str, fontSize := fSize).render)
  }

  protected def getBox(e: Locatable): Rectangle = {
    s.appendChild(e)
    val box = e.getBBox()
    s.removeChild(e)
    box
  }

  protected lazy val agentGradient =
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

  def drawAgent(agent: KappaModel.Agent): TypedTag[SVG] = {
    val b: Rectangle = getTextBox(agent.name, agentFontSize)
    //val (w, h) = (b.width + agentPadding * 2, b.height + agentPadding)
    drawAgent(agent, b.copy(width = b.width, height = b.height))
    //val agent = dra this.addNode(id, data, new ViewOfNode(data, new HtmlSprite(element), colorName))
  }

  /**
    * Aligns horizontally and returns new parent width
    *
    * @param list
    * @param padding
    * @param parentWidth
    * @return
    */
  protected def alignHor(list: List[(Side, Rectangle)], padding: Double, parentWidth: Double): (List[(Side, Rectangle)], Double) = if(list.isEmpty) (Nil, parentWidth) else {
    val rev: List[(Side, Rectangle)] = list.foldLeft(List.empty[(Side, Rectangle)]) {
      case (Nil, (sd, box)) =>
        (sd, box.copy(x = 0))::Nil

      case ( (hs, hbox) :: tail, (sd, box)) =>
        val nx = hbox.right + padding * 2
        //println("NX = "+ nx+" and x = "+hbox.left)
        (sd, box.copy(x = nx))::(hs, hbox)::tail
    }
    val totalWidth = rev.head._2.right
    val start = Math.max((parentWidth - totalWidth) / 2, 0)
    //reversing back and shifting according to start value
    rev.foldLeft(List.empty[(Side, Rectangle)]) {
       case ( acc, (sd, box)) =>
         val nx = box.x + start
        (sd, box.copy(x = nx))::acc
    } -> Math.max(rev.head._2.right, parentWidth)
  }

  protected def renderWithSides(agent: KappaModel.Agent, box: Rectangle, border: SideBorder): TypedTag[SVG] = {
    require(border.top.nonEmpty, "SideBorder should always have an element on top")

    val rect = Rectangle(box.width, box.height).withPadding(agentPadding * 2, agentPadding).copy(x = 0)
    val (rectTop, mergedTop, topSides)= renderTop(border, rect, rect)
    val (rectBottom, mergedBottom, bottomSides)= renderBottom(border, rectTop, mergedTop)

    val agentLabel = drawLabel(agent.name, rectBottom, box, agentFontSize, "GradAgent")
    drawSprite(mergedBottom.width, mergedBottom.height, agentLabel :: topSides ::: bottomSides)
  }

  /**
    * Renders top raw of the border
    * @param border
    * @param rect
    * @return
    */
  def renderTop(border: SideBorder, rect: Rectangle, merged: Rectangle) = if(border.top.isEmpty) (rect, merged, Nil) else {
    val (top, r) = alignHor(border.top, sidePadding, rect.width)
    val sides: List[TypedTag[SVG]] = for {
      (side, box) <- top
    } yield {
      val sideBox = box.copy(y = 0).withPadding(sidePadding, sidePadding)
      drawLabel(side.name, sideBox, box, sideFontSize, "GradSide")
    }
    val sideTextBox: Rectangle = top.head._2.copy(y = 0, x = 0)
    val rectAdj = rect.copy(y = sideTextBox.height, width = Math.max(r, rect.width))
    //val merged = rect.copy(y = sideTextBox.height, width = r).merge(sideTextBox)
    (rectAdj, merged.merge(rectAdj), sides)
  }


  /**
    * Renders bottom raw of the border
    * @param border
    * @param rect
    * @return
    */
  def renderBottom(border: SideBorder, rect: Rectangle, merged: Rectangle) = if(border.bottom.isEmpty) (rect, merged, Nil) else {
    val (bottom, r) = alignHor(border.bottom, sidePadding, rect.width)
    val sides: List[TypedTag[SVG]] = for {
      (side, box) <- bottom
    } yield {
      val sideBox = box.copy(y = 0).withPadding(sidePadding, sidePadding)
      drawLabel(side.name, sideBox, box, sideFontSize, "GradSide")
    }
    val sideTextBox: Rectangle = bottom.head._2.copy(y = rect.bottom, x = 0)
    val rectAdj = rect.copy(width = Math.max(r, rect.width))
    //val merged = rect.copy(y = sideTextBox.height, width = r).merge(sideTextBox)
    (rectAdj, merged.merge(rectAdj).copy(height = rectAdj.height + sideTextBox.height), sides)
  }

  /**
    * Renders right raw of the border
    * @param border
    * @param rect
    * @return
    */
  def renderRight(border: SideBorder, rect: Rectangle) = if(border.right.isEmpty) (rect, Nil) else {
    val (side, box) = border.right.head
    val sideBox = box.copy(x = rect.right, y = rect.y)
    val sprite = drawLabel(side.name, sideBox, box, sideFontSize, "GradSide")
    (rect.merge(sideBox), List(sprite))
    //(merged, sides)
  }

  /**
    * Renders right raw of the border
    * @param border
    * @param rect
    * @return
    */
  def renderLeft(border: SideBorder, rect: Rectangle) = if(border.right.isEmpty) (rect, Nil) else {
    val (side, box) = border.left.head
    val sprite = drawLabel(side.name, box, box, sideFontSize, "GradSide")
    (rect.copy(width = box.width + rect.width), List(sprite))
    //(merged, sides)
  }



  protected def drawAgent(agent: KappaModel.Agent, box: Rectangle) = agent.sides match {
    case Nil =>
      val rect = Rectangle(box.width, box.height).withPadding(agentPadding * 2, agentPadding)
      val agentLabel = drawLabel(agent.name, rect, box, agentFontSize, "GradAgent")
      drawSprite(rect.width, rect.height, List(agentLabel))

    //drawBox(agent.name, rect.width, rect.height, agentFontSize, agentPadding * 2, agentPadding)

    case sides =>
      val (boxes: List[(KappaModel.Side, Rectangle)], w: Double) = sides.foldLeft(List.empty[(KappaModel.Side, Rectangle)], 0.0) {
        case ((list, cw), sd) =>
          val str = sd.name
          val box = getTextBox(str, sideFontSize)
          val nw = cw + box.width + sidePadding * 3
          ((sd, box) :: list, nw)
      }
      val border = SideBorder.extract(box.width, boxes.reverse, w, sidePadding)
      renderWithSides(agent, box, border)
/*
      val rect = Rectangle(box.width, box.height).withPadding(agentPadding * 2, agentPadding)
      val agentLabel = drawLabel(agent.name, rect, box, agentFontSize, agentPadding * 2, agentPadding)
      drawSprite(rect.width, rect.height, List(agentLabel))
      */
  }

  protected def drawLabel(str: String, rectangle: Rectangle, textBox: Rectangle,
                          fSize: Double, grad: String): TypedTag[SVG] = {
    val st = 1
    val r = rect(
      stroke := "blue",
      fill := s"url(#${grad})",
      strokeWidth := st,
      svgAttrs.height := rectangle.height,
      svgAttrs.width := rectangle.width,
      rx := 50, ry := 50
    )
    val startX = (rectangle.width - textBox.width) / 2
    val startY = (rectangle.height - textBox.height) / 2 + textBox.height
    val txt = text(str, fontSize := fSize, x := startX, y := startY)
    import scalatags.JsDom.implicits._

    svg(
      svgAttrs.height := rectangle.height + st,
      svgAttrs.width := rectangle.width + st,
      svgAttrs.x := rectangle.x,
      svgAttrs.y := rectangle.y,
      r, txt
    )
  }

  protected def drawSprite(w: Double, h: Double, children: List[JsDom.Modifier]): TypedTag[SVG] = {
    val decs: JsDom.Modifier = defs(agentGradient, sideGradient)
    val params = List(
      svgAttrs.height := h,
      svgAttrs.width := w,
      decs
    ) ++ children
    svg.apply(params: _*)
  }

}
