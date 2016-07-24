package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.binding.extensions._
import org.denigma.kappa.model.KappaModel.{Agent, KappaNamedElement, _}
import org.denigma.kappa.notebook.views.visual.rules.drawing.SvgBundle.all._
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.kappa.notebook.views.visual.rules.drawing.SvgBundle.all.attrs._
import org.denigma.kappa.notebook.views.visual.rules.drawing.{KappaPainter, Rectangle}
import org.denigma.threejs.Object3D
import org.denigma.threejs.extras.HtmlSprite
import org.scalajs.dom.svg.{LinearGradient, SVG}

import scalatags.JsDom.TypedTag

trait KappaView extends KappaPainter {

  def label: String

  //type Data <: KappaNamedElement

  def fontSize: Double
  def padding: Double
  protected def gradientName: String
  //protected def gradient: TypedTag[LinearGradient]
  def gradient:  Var[TypedTag[LinearGradient]]

  lazy val textBox = getTextBox(label, fontSize)

  lazy val labelBox = textBox.withPadding(padding, padding)

  protected val svg = Rx {
    labelStroke()
    val grad = gradient()
    val rect = Rectangle(textBox.width, textBox.height).withPadding(padding * 2, padding)
    val lb: TypedTag[SVG] = drawLabel(label, rect, textBox, fontSize, gradientName)
    drawSVG(rect.width, rect.height, List(gradient.now), List(lb))
  }

  protected val sprite: Rx[HtmlSprite] = svg.map {
    case s => new HtmlSprite(s.render)
  }

  protected val spriteChange = sprite.zip
  spriteChange.onChange{
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

class KappaAgentView(val label: String, val fontSize: Double, val padding: Double, val s: SVG) extends KappaView
{

  type ChildView = KappaSightView

  override def gradientName: String = "GradAgent"

  protected lazy val defaultGradient: TypedTag[LinearGradient] =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "skyblue"),
      stop(offset := "50%", stopColor := "deepskyblue"),
      stop(offset := "100%", stopColor := "SteelBlue")
    )

  lazy val gradient = Var(defaultGradient)

  //lazy val children: List[KappaSightView] = data.sides.map(side=> new KappaSightView(side, fontSize / 1.6, padding / 2, s))
}

class KappaSightView(val label: String, val fontSize: Double, val padding: Double, val s: SVG) extends  KappaView
{

  type Data = Sight

  type ChildView = KappaStateView


  override def gradientName: String =  "GradSide"

  protected lazy val defaultGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )

  lazy val gradient = Var(defaultGradient)

  //lazy val children: List[ChildView] = data.states.toList.map(state=> new KappaStateView(state, fontSize / 1.6, padding / 1.6, s))

}

class KappaStateView(val label: String, /*val data: State,*/ val fontSize: Double, val padding: Double, val s: SVG) extends KappaView
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