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
  val shrinkMult = Var(2)

  val active: rx.Rx[Boolean] = selected.map(value => value == this.id)
  val scaleX = Var(FlexibleLinearScale("Time", 0.0, 10, 2, 400))
  val scaleY = Var(FlexibleLinearScale("Concentration", 0.0, 10, 2, 600, inverted = true))

  val empty: rx.Rx[Boolean] = items.map(_.isEmpty)

  def max(series: Series)(fun: Point=>Double): Point = series.points.maxBy(fun)

  val max = items.map{case its=>
    val x = its.foldLeft(0.0){ case (acc, series)=> series.now.points.maxBy(_.x).x}
    val y = its.foldLeft(0.0){ case (acc, series)=> series.now.points.maxBy(_.y).y}
    Point(x, y)
  }
  max.foreach{
    case value=>
      if(flexible()) {
        val (sX, sY) = (scaleX.now, scaleY.now)
        val sh = shrinkMult()
        val updScaleX = sX.stretched(value.x, shrinkMult = sh)
        scaleX.set(updScaleX)
        val updScaleY = sY.stretched(value.y, shrinkMult = sh)
        scaleY.set(updScaleY)
        println(s"UPD SCALEX =$updScaleX")
      }
  }

  override def newItemView(item: Item): SeriesView = constructItemView(item){
    case (el, mp) => new SeriesView(el, item, transform).withBinder(new GeneralBinder(_))
  }
}


case class FlexibleLinearScale(title: String, start: Double, end: Double, stepSize: Double, length: Double, inverted: Boolean = false, precision:Int = 3) extends WithLinearScale
{
  def truncateAt(n: Double, p: Int): Double = if(p>0) { val s = math pow (10, p); (math floor n * s) / s } else n

  override def step(value: Double): Double = truncateAt(value + stepSize, precision)

  override def points(current: Double, end: Double, dots: List[Double] = List.empty): List[Double]  = {
    if (current<end) points(truncateAt(step(current), precision), end, current::dots) else (truncateAt(end, precision)::dots).reverse
  }


  if(stepSize > Math.abs(start - end)) dom.console.error(s"stepSize is larger then ")

  /**
    *
    * @param max maximum value of the point coordinate
    * @param stretchMult makes end strechMult times more then maximum value
    * @param shrinkMult shrinks the scale if maximum is much larger then end
    * @return
    */
  def stretched(max: Double, stretchMult: Double = 1.2, shrinkMult: Int = -1): FlexibleLinearScale = if(max > end) {
    val newEnd = max * stretchMult
    //println(s"maximizing from ${scale.end} to ${newEnd}")
    this.copy(end = newEnd, stepSize = newEnd / ticks.size)
  } else if( shrinkMult > 0 && Math.abs(max - start) > 0.0 && end > max * shrinkMult){
    this.copy(end = max * stretchMult, stepSize = stepSize / 2)
  } else this //does not change anything

}

trait WithLinearScale extends Scale {

  def stepSize: Double

  def inverted: Boolean

  override def step(value: Double): Double = value + stepSize

  lazy val scale: Double = length / (end - start)

  def inverse(value: Double): Double = end - value + start

  def coord(value: Double): Double = if(inverted) inverse(value) * scale else value * scale

  //real coord to chart coord (it implies that 0 is the same
  override def chartCoord(coord: Double): Double = if(inverted) inverse(coord / scale) else coord / scale + start
}