package org.denigma.kappa.notebook.views.visual.rules.drawing
import org.denigma.kappa.notebook.views.visual.rules.drawing.SvgBundle.all._
import rx._
import org.denigma.kappa.notebook.views.visual.rules.drawing.SvgBundle.all.attrs._
import org.denigma.threejs.{Side => _}
import org.scalajs.dom.raw.{SVGElement, SVGLocatable}
import org.scalajs.dom.svg.SVG

import scalatags.JsDom
import scalatags.JsDom.TypedTag
trait KappaPainter {

  def s: SVG

  type Locatable = SVGElement with SVGLocatable

  lazy val labelStroke:  Var[String] = Var("blue")

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
    val st = 2
    val r = rect(
      stroke :=  labelStroke.now,
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
