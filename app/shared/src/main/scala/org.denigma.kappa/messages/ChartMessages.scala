package org.denigma.kappa.messages


import org.denigma.controls.charts.{LineStyles, Point, Series}
import org.denigma.kappa.messages.WebSimMessages.KappaPlot

import scala.collection.immutable._

object KappaChart {
  lazy val empty = KappaChart(List.empty)

  implicit def fromKappaPlot(plot: KappaPlot): KappaChart = {
    val series = plot.legend.zipWithIndex.map{ case (title, i) =>
      //println("title: " + title)
      KappaSeries(title, plot.time_series.map(o=> Point(o.observation_time, o.observation_values(i))).toList) }
    KappaChart(series.toList)
  }
}

/**
  * @param series of kappa data
  */
case class KappaChart(series: List[KappaSeries])
{
  def isEmpty: Boolean = series.isEmpty
}


object KappaSeries {

  import scala.util.Random

  def randomColor() = s"rgb(${Random.nextInt(255)},${Random.nextInt(255)},${Random.nextInt(255)})"

  def randomLineStyle() = LineStyles(randomColor(), 4 ,"none" , 1.0)

}


case class KappaSeries(title: String, points: List[Point], style: LineStyles = KappaSeries.randomLineStyle()) extends Series