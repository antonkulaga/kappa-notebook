package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.kappa.notebook.graph.KappaView
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all._
import rx._
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all.attrs._
import org.scalajs.dom.svg.{LinearGradient, SVG}

import scalatags.JsDom.TypedTag

class KappaAgentView(val label: String, val fontSize: Double, val padding: Double, val s: SVG) extends KappaView
{

  override def gradientName: String = "GradAgent"

  protected lazy val defaultGradient: TypedTag[LinearGradient] =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "skyblue"),
      stop(offset := "50%", stopColor := "deepskyblue"),
      stop(offset := "100%", stopColor := "SteelBlue")
    )

  lazy val gradient = Var(defaultGradient)
}

class KappaSiteView(val label: String, val fontSize: Double, val padding: Double, val s: SVG) extends  KappaView
{

  override def gradientName: String =  "GradSide"

  protected lazy val defaultGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )

  lazy val gradient = Var(defaultGradient)

}

class KappaStateView(val label: String, val fontSize: Double, val padding: Double, val s: SVG) extends KappaView
{

  lazy val gradientName = "GradModif"

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