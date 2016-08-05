package org.denigma.kappa.notebook.views.visual.rules
import org.denigma.kappa.notebook.graph._
import org.scalajs.dom.svg.SVG

object RulesVisualSettings
{
  val DEFAULT_AGENT_FONT = 20
  val DEFAULT_SITE_FONT = 14
  val DEFAULT_STATE_FONT = 11
}

case class RulesVisualSettings(
                                canvas: SVG,
                                agent: KappaNodeVisualSettings = KappaNodeVisualSettings(RulesVisualSettings.DEFAULT_AGENT_FONT, 5),
                                sites: KappaNodeVisualSettings  = KappaNodeVisualSettings(RulesVisualSettings.DEFAULT_SITE_FONT, 3),
                                state: KappaNodeVisualSettings = KappaNodeVisualSettings(RulesVisualSettings.DEFAULT_STATE_FONT, 2),
                                link: KappaEdgeVisualSettings = KappaEdgeVisualSettings(10, 1, LineParams(lineColor = Colors.blue)),
                                otherArrows: LineParams = LineParams(lineColor = 0x000000)
                              )
