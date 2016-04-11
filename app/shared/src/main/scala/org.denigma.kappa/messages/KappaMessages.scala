package org.denigma.kappa.messages

import boopickle.Default._
import org.denigma.controls.charts.{LineStyles, Point, Series}
import org.denigma.kappa.WebSim
import org.denigma.kappa.WebSim.KappaPlot

import scala.collection.immutable._

object KappaChart {
  lazy val empty = KappaChart(List.empty)

  implicit def fromKappaPlot(plot: WebSim.KappaPlot): KappaChart = {
    val series = plot.legend.zipWithIndex.map{ case (title, i) =>
      //println("title: " + title)
      KappaSeries(title, plot.observables.map(o=> Point(o.time, o.values(i))).toList) }
    KappaChart(series.toList)
  }
}


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
