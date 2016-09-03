package org.denigma.kappa.notebook.graph


import org.denigma.binding.extensions._
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all.attrs._
import org.denigma.kappa.notebook.graph.drawing.{KappaPainter, Rectangle}
import org.denigma.threejs.Object3D
import org.denigma.threejs.extras.HtmlSprite
import org.scalajs.dom.svg.{LinearGradient, SVG}

import scalatags.JsDom.TypedTag

trait KappaView extends KappaPainter {

  def label: String

  def fontSize: Double
  def padding: Double
  protected def gradientName: String
  //protected def gradient: TypedTag[LinearGradient]
  def gradient:  Var[TypedTag[LinearGradient]]

  lazy val textBox = getTextBox(label, fontSize)

  lazy val labelBox = textBox.withPadding(padding, padding)

  def opacity = sprite.now.element.style.opacity
  def opacity_=(value: Double) = {
    this.sprite.now.element.style.opacity = value.toString
    render()
  }

  protected val svg = Rx {
    labelStrokeColor()
    val st = labelStrokeWidth()
    val grad = gradient()
    val rect = Rectangle(textBox.width, textBox.height).withPadding(padding * 2, padding)
    val lb: TypedTag[SVG] = drawLabel(label, rect, textBox, fontSize, gradientName)
    drawSVG(rect.width + st *2, rect.height + st *2 , List(gradient.now), List(lb))
  }

  lazy val sprite: Rx[HtmlSprite] = svg.map {
    s =>
      val sp = new HtmlSprite(s.render)
      sp
  }

  protected val spriteChange = sprite.zip
  spriteChange.foreach{
    case (old, n)=>
      if(old != n) {
        container.remove(old)
        container.add(n)
      }
  }

  lazy val container = new Object3D()

  def render(): Object3D = {
    clearChildren()
    container.add(sprite.now)
    container
  }

  def clearChildren() = {
    container.children.toList.foreach(container.remove)
  }

  render()
}

class KappaEdgeView(val label: String, val fontSize: Double, val padding: Double, val s: SVG) extends KappaView
{
  lazy val gradientName = "GradModif"

  protected lazy val defaultGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )

  lazy val gradient = Var(defaultGradient)
}
