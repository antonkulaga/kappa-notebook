package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.kappa.notebook.graph._
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.controls.drawing.SvgBundle.all._
import org.denigma.controls.drawing.SvgBundle.all.attrs._
import org.scalajs.dom.svg.SVG



class RuleFluxEdgeView(val label: String, val fontSize: Double, val padding: Double, lines: LineParams, val percent: Rx[Double], val s: SVG) extends  KappaView
{

  override def gradientName: String =  "RuleFluxEdge"

  protected lazy val defaultGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )

  lazy val gradient = Var(defaultGradient)

}



class FluxEdge(val from: FluxNode, val to: FluxNode, val value: Double,  min: Rx[Double], max: Rx[Double])(implicit val create: FluxEdge => RuleFluxEdgeView) extends LineEdge{
  self =>

  lazy val percent = Rx{
    val  minValue = min()
    val maxValue = max()
    (value, maxValue, minValue) match {
      case (v, mx, _) if v >= mx => 1.0 //when maximum is not yet updated
      case (v, mx, _) if v > 0 => v / mx
      case (v, _, mi) if v < 0 => v / mi
      case _ => 0.0
    }
  }
  percent.foreach(value =>
    material.linewidth = 1 + Math.round(value * 4)
  )

  override type FromNode = FluxNode
  override type ToNode = FluxNode

  val view: RuleFluxEdgeView = create(this)

  lazy val color = if(Math.abs(value) < 1) Colors.blue else if(value> 0) Colors.green else Colors.red

  lazy val lineParams = LineParams(lineColor = self.color, thickness = 2)

  override def update() = {
    posLine()
    posSprite()
    opacity =  if(Math.abs(percent.now) <= 0.1) 0.8 else 1.0
  }

  def posSprite() = {
    val m = middle
    view.container.position.set(m.x, m.y, m.z)
  }

}