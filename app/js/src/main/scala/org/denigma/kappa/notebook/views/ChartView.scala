package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.controls.charts._
import org.denigma.controls.tabs.{TabItemView, TabItem}
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx.core.{Var, Rx}
import rx.ops._
import scala.collection.immutable._

class ChartView(val elem: Element,
                val items: Rx[Seq[Rx[Series]]],
                val selected: Var[String]
                  ) extends LinesPlot {

  val flexible = Var(true)
  val shrinkMult = Var(1.05)
  val stretchMult = Var(1.05)

  val active: rx.Rx[Boolean] = selected.map(value => value == this.id)
  val scaleX = Var(FlexibleLinearScale("Time", 0.0, 10, 2, 400))
  val scaleY = Var(FlexibleLinearScale("Concentration", 0.0, 10, 2, 500, inverted = true))

  val empty: rx.Rx[Boolean] = items.map(_.isEmpty)

  def max(series: Series)(fun: Point=>Double): Point = series.points.maxBy(fun)

  val max = items.map{case its=>
    val x = its.foldLeft(0.0){ case (acc, series)=> Math.max(acc, series.now.points.maxBy(_.x).x)}
    val y = its.foldLeft(0.0){ case (acc, series)=> Math.max(acc, series.now.points.maxBy(_.y).y)}
    Point(x, y)
  }
  max.foreach{
    case Point(x, y) =>
      if(flexible()) {
        val (sX, sY) = (scaleX.now, scaleY.now)
        val sh = shrinkMult()
        val st = stretchMult()
        val updScaleX = sX.stretched(x, stretchMult = st, shrinkMult = sh)
        scaleX.set(updScaleX)
        val updScaleY = sY.stretched(y,  stretchMult = st, shrinkMult = sh)
        scaleY.set(updScaleY)
        //println("TICK LEN = "+scaleX.now.ticks.length)
      }
  }

  override def newItemView(item: Item): SeriesView = constructItemView(item){
    case (el, mp) => new SeriesView(el, item, transform).withBinder(new GeneralBinder(_))
  }
}

case class FlexibleLinearScale(title: String, start: Double, end: Double, stepSize: Double, length: Double, inverted: Boolean = false, precision:Int = 3) extends WithLinearScale
{

  lazy val span =  Math.abs(end - start)

  override lazy val scale = length / span

  if(stepSize > Math.abs(start - end)) dom.console.error(s"stepSize is larger than length of the axis")

  override def points(current: Double, end: Double, dots: List[Double] = List.empty): List[Double]  = {
    val tick = step(current)
    if (current<end) points(truncateAt(tick, precision), end, current::dots) else (truncateAt(end, precision)::dots).reverse
  }


  /**
    *
    * @param max maximum value of the point coordinate
    * @param stretchMult makes end strechMult times more then maximum value
    * @param shrinkMult shrinks the scale if maximum is much larger then end
    * @return
    */
  def stretched(max: Double, stretchMult: Double = 1.1, shrinkMult: Double = -1): FlexibleLinearScale = if(max > end) {
    val newEnd = max * stretchMult
    val st = Math.abs(newEnd - start) / (ticks.length - 2)
    this.copy(end = newEnd, stepSize = st)
  } else if( shrinkMult > 0 && Math.abs(max - start) > 0.0 && end > max * shrinkMult){
    val newEnd = max
    val st = Math.abs(newEnd - start) / (ticks.length - 2)
    this.copy(end = newEnd, stepSize = st)
  } else this //does not change anything


}