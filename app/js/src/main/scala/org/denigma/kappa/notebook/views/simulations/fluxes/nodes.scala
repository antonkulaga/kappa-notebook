package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.drawing.{KappaPainter, Rectangle}
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all._
import org.denigma.kappa.notebook.graph.layouts._
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all.attrs._
import org.denigma.threejs.extras.HtmlSprite
import org.scalajs.dom
import org.scalajs.dom.raw.{ClientRect, Element, HTMLElement}
import org.scalajs.dom.svg.{LinearGradient, SVG}

import scala.collection.immutable._
import scalatags.JsDom.TypedTag



trait SimpleKappaView extends KappaPainter
{
  def label: String

  def fontSize: Double
  def padding: Double
  protected def gradientName: String
  def gradient:  Var[TypedTag[LinearGradient]]

  lazy val textBox = getTextBox(label, fontSize)

  lazy val labelBox = textBox.withPadding(padding, padding)

  def opacity = sprite.now.element.style.opacity
  def opacity_=(value: Double) = {
    this.sprite.now.element.style.opacity = value.toString
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
      //for debugging
      sp.position.x = 0
      sp.position.y = 0
      sp.position.z = 0
      sp
  }
  def container = sprite.now
  sprite.zip.onChange{ case (prev, cur) =>
    if(prev!=cur) dom.console.log(s"chage of sprite to ${cur.element.outerHTML}")
    /*
      if(prev!=cur) prev.parent match {
        case p if p!=null && !scalajs.js.isUndefined(p) =>
          p.remove(prev)
          p.add(cur)
        case _ =>
      }
      */
  }
}



class RuleFluxView(val label: String, val fontSize: Double, val padding: Double, val s: SVG) extends SimpleKappaView {
  override def gradientName: String = "RuleFluxView"

  protected lazy val defaultGradient: TypedTag[LinearGradient] =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "skyblue"),
      stop(offset := "50%", stopColor := "deepskyblue"),
      stop(offset := "100%", stopColor := "SteelBlue")
    )

  lazy val gradient = Var(defaultGradient)
}

class FluxNode(val flux: RuleFlux)(implicit val create: FluxNode => RuleFluxView ) extends ForceNode {
  val view = create(this)
  val layoutInfo: LayoutInfo = new LayoutInfo(1)
  override def position = view.container.position

}