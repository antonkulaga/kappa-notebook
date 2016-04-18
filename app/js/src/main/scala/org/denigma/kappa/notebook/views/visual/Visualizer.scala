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

  private def splitByWidth(sides: List[(Side, Rectangle)], w: Double, padding: Double) =
  {
    val (t, r, s) = sides.foldLeft((List.empty[(Side, Rectangle)], List.empty[(Side, Rectangle)], 0.0)) {
      case ( (taken, rest, sm), (side, rect)) =>
        if( (sm + padding) > w) (taken, (side, rect)::rest, sm)
        else {
          val curSm = sm + padding + rect.width
          ((side, rect)::taken, rest, sm)
        }
    }
    (t.reverse, r.reverse, s) //in foldLeft we collected them in reverse way

  }

  def extract(parentWidth: Double, sides: List[(Side, Rectangle)], totalWidth: Double, padding: Double = 5) = totalWidth match {
    case w if w <= parentWidth * 2 =>
      val (top, bottom, sm) = splitByWidth(sides, parentWidth, 5)
      SideBorder(top, bottom)

    case w =>
      val (top, right::rest, _) = splitByWidth(sides, parentWidth, padding)
      rest match {
        case Nil => SideBorder(top, List(right)) //if right is left better to add it to the bottom
        case smth::Nil => SideBorder(top, List(right), List(smth))

        case other =>   splitByWidth(other, parentWidth, padding) match {
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

  protected def drawAgent(agent: KappaModel.Agent, box: Rectangle) = agent.sides match {
    case Nil =>
      val rect = Rectangle(box.width, box.height).withPadding(agentPadding * 2, agentPadding)
      val agentLabel = drawLabel(agent.name, rect, box, agentFontSize, agentPadding * 2, agentPadding)
      drawSprite(rect.width, rect.height, List(agentLabel))

    //drawBox(agent.name, rect.width, rect.height, agentFontSize, agentPadding * 2, agentPadding)

    case side :: Nil =>
      val rect = Rectangle(box.width, box.height).withPadding(agentPadding * 2, agentPadding).copy(x = 0)
      val sideTextBox = getTextBox(side.name, sideFontSize).copy(y = 0.0)
      val sideBox = sideTextBox.withPadding(sidePadding, sidePadding).centerHor(rect)
      val sideLabel = drawLabel(side.name, sideBox, sideTextBox, sideFontSize, sidePadding, sidePadding, "GradSide")
      val rectAdj = rect.copy(y = sideBox.height)
      val merged = rectAdj.merge(sideBox)
      println("reactAdj = "+ rectAdj + " and merged"+merged)
      val agentLabel = drawLabel(agent.name, rectAdj, box, agentFontSize, agentPadding * 2, agentPadding, "GradAgent")
      drawSprite(merged.width, merged.height, List(agentLabel, sideLabel))

    case side :: others =>
      val (boxes: List[(KappaModel.Side, Rectangle)], w: Double) = agent.sides.foldLeft(List.empty[(KappaModel.Side, Rectangle)], 0.0) {
        case ((list, cw), side) =>
          val str = side.name
          val svg = text(fontSize := sideFontSize, str)
          val box = getBox(svg.render)
          ((side, box) :: list, cw + box.width)
      }
      //println(s"8888888888\n drawAround = \n" +(rect.width, boxes.reverse, w))
      val rect = Rectangle(box.width, box.height).withPadding(agentPadding * 2, agentPadding).copy(x = 0)
      val sideTextBox = getTextBox(side.name, sideFontSize).copy(y = 0.0)
      val sideBox = sideTextBox.withPadding(sidePadding, sidePadding).centerHor(rect)
      val sideLabel = drawLabel(side.name, sideBox, sideTextBox, sideFontSize, sidePadding, sidePadding, "GradSide")
      val rectAdj = rect.copy(y = sideBox.height)
      val merged = rectAdj.merge(sideBox)
      println("reactAdj = "+ rectAdj + " and merged"+merged)
      val agentLabel = drawLabel(agent.name, rectAdj, box, agentFontSize, agentPadding * 2, agentPadding, "GradAgent")
      drawSprite(merged.width, merged.height, List(agentLabel, sideLabel))


    //renderBorder(SideBorder.extract(rect.width, boxes.reverse, w))

  }

  protected def renderBorder(border: SideBorder): Unit = {
    border.top.foreach {
      case (side, box) =>
    }
  }


  protected def drawLabel(str: String, rectangle: Rectangle, textBox: Rectangle,
                          fSize: Double, horPadding: Double,
                          verPadding: Double, grad: String = "GradAgent"): TypedTag[SVG] = {
    val st = 1
    val r = rect(
      stroke := "blue",
      fill := s"url(#${grad})",
      strokeWidth := st,
      svgAttrs.height := rectangle.height,
      svgAttrs.width := rectangle.width,
      rx := 50, ry := 50
    )
    val txt = text(str, fontSize := fSize, x := horPadding, y := (rectangle.height / 2.0 + textBox.height / 2.0 ))
    svg(
      all.height := rectangle.height + st,
      all.width := rectangle.width + st,
      x := rectangle.x,
      y := rectangle.y,
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

class Visualizer (val container: HTMLElement,
                  val width: Double,
                  val height: Double,
                  val layouts: Rx[Seq[GraphLayout]],
                  agentFontSize: Double,
                  padding: Double,
                  override val distance: Double
                 )
  extends Container3D with Randomizable
{

  override val controls: JumpCameraControls = new  JumpCameraControls(camera, this.container, scene, width, height)

  override def defRandomDistance = distance * 0.6

  def randomPos(obj: Object3D): Vector3 =  obj.position.set(rand(),rand(),rand())

  def onMouseDown(obj: Object3D)(event: MouseEvent ):Unit =  if(event.button==0)
  {
    this.controls.moveTo(obj.position)
  }

  //https://github.com/antonkulaga/semantic-graph/tree/master/graphs/src/main/scala/org/denigma/graphs

  import scalatags.JsDom.all.id

  lazy val s: SVG = {
    val t = svg().render
    t.style.position = "absolute"
    t.style.top = "-9999"
    t
  }
  container.appendChild(s)

  lazy val painter = new AgentPainter(agentFontSize, padding, s)

  def addAgent(agent: KappaModel.Agent) = {
    val txt = text(agent.name, fontSize := agentFontSize)
    //val b: SVGRect = getBox(txt.render)
    val sprite = painter.drawAgent(agent)
    //val sprite = drawBox(agent.name)
    val sp = new HtmlSprite(sprite.render)
    randomPos(sp)
    addSprite(sp)
    sp
  }

  def addSprite(htmlSprite: HtmlSprite) = {
    cssScene.add(htmlSprite)
  }

  override def onEnterFrame() = {
    this.layouts.now.foreach{case l=>
      if(l.active) l.tick()
      //dom.console.info(s"l is ${l.active}")
    }
    super.onEnterFrame()
  }

}

