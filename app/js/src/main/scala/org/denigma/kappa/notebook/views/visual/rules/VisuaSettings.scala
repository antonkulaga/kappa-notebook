package org.denigma.kappa.notebook.views.visual.rules
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.kappa.model.KappaModel.Agent
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.layouts._
import org.scalajs.dom
import org.scalajs.dom.raw.{ClientRect, Element}
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._
import scala.collection.immutable._

object RulesVisualSettings
{
  val DEFAULT_AGENT_FONT = 32
  val DEFAULT_SITE_FONT = 20
  val DEFAULT_STATE_FONT = 14
}

case class RulesVisualSettings(
                                canvas: SVG,
                                agent: KappaNodeVisualSettings = KappaNodeVisualSettings(RulesVisualSettings.DEFAULT_AGENT_FONT, 5),
                                sites: KappaNodeVisualSettings  = KappaNodeVisualSettings(RulesVisualSettings.DEFAULT_SITE_FONT, 3),
                                state: KappaNodeVisualSettings = KappaNodeVisualSettings(RulesVisualSettings.DEFAULT_STATE_FONT, 2),
                                link: KappaEdgeVisualSettings = KappaEdgeVisualSettings(14, 4, LineParams(lineColor = Colors.blue)),
                                otherArrows: LineParams = LineParams(lineColor = 0x000000)
                              )
