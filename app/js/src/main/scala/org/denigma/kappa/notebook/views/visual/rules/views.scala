package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.kappa.notebook.graph.KappaView
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all._
import rx._
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all.attrs._
import org.scalajs.dom.svg.{LinearGradient, SVG}

import scalatags.JsDom.TypedTag

object KappaAgentView {
  def gradientName: String = "GradAgent"
}

class KappaAgentView(val label: String, val fontSize: Double, val padding: Double, val nodeGradient: TypedTag[LinearGradient], val s: SVG) extends KappaView
{

  override def gradientName: String = KappaAgentView.gradientName

  lazy val gradient = Var(nodeGradient)
}

object KappaSiteView {
  def gradientName = "GradSide"
}

class KappaSiteView(val label: String, val fontSize: Double, val padding: Double, val siteGradient: TypedTag[LinearGradient], val s: SVG) extends  KappaView
{

  override def gradientName: String =  KappaSiteView.gradientName


  lazy val gradient = Var(siteGradient)

}

object KappaStateView {
  def gradientName = "GradModif"
}

class KappaStateView(val label: String, val fontSize: Double, val padding: Double, val s: SVG) extends KappaView
{

  lazy val gradientName = KappaStateView.gradientName

  protected lazy val defaultGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "ivory")
    )

  lazy val gradient = Var(defaultGradient)


}

class KappaLinkView(val label: String, val fontSize: Double, val padding: Double, val s: SVG) extends  KappaView
{

  override def gradientName: String =  "GradSide"

  protected lazy val defaultGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "ivory")
    )

  lazy val gradient = Var(defaultGradient)

}