package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.kappa.notebook.graph.KappaView
import org.denigma.controls.drawing.SvgBundle.all._
import rx._
import org.denigma.controls.drawing.SvgBundle.all.attrs._
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
  def gradientName = "GradSite"
}

class KappaSiteView(val label: String, val fontSize: Double, val padding: Double, val siteGradient: TypedTag[LinearGradient], val s: SVG) extends  KappaView
{

  override def gradientName: String =  KappaSiteView.gradientName

  lazy val gradient = Var(siteGradient)

}

object KappaStateView {
  def gradientName = "GradState"
}

class KappaStateView(val label: String, val fontSize: Double, val padding: Double, val stateGradient: TypedTag[LinearGradient], val s: SVG) extends KappaView
{

  lazy val gradientName = KappaStateView.gradientName

  lazy val gradient = Var(stateGradient)


}
/*

object KappaLinkView {
  def gradientName = "GradLink"
}

class KappaLinkView(val label: String, val fontSize: Double, val padding: Double, val linkGradient: TypedTag[LinearGradient], val s: SVG) extends  KappaView
{

  override def gradientName: String =  KappaLinkView.gradientName

  lazy val gradient = Var(linkGradient)

}
*/