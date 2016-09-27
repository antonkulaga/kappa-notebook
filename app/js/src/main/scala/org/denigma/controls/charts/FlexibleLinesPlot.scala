package org.denigma.controls.charts
import org.denigma.binding.extensions._
import org.scalajs.dom
import rx.Var

import scala.collection.immutable.List
//import rx.Ctx.Owner.voodoo
import rx.Ctx.Owner.Unsafe.Unsafe
/**
  * Created by antonkulaga on 9/26/16.
  */


case class FlexibleLinearScale(title: String, start: Double, end: Double, stepSize: Double, length: Double, inverted: Boolean = false, precision:Int = 3)
  extends WithLinearScale with WithFlexibleScale[FlexibleLinearScale]
{

  private lazy val currentLengh = Math.abs(start - end)

  if(stepSize > Math.max(currentLengh, Math.round(currentLengh))) {
    dom.console.error(s"stepSize(${stepSize}) is larger then currentLength(${currentLengh})")
  }

  override def points(current: Double, end: Double, dots: List[Double] = List.empty): List[Double]  = {
    val tick = step(current)
    if (current<end) points(truncateAt(tick, precision), end, current::dots) else dots.reverse
  }

  private def betterStep(st: Double): Double = if (st > 1) Math.round(st) else st

  /**
    *
    * @param max maximum value of the point coordinate
    * @param stretchMult makes end strechMult times more then maximum value
    * @param shrinkMult shrinks the scale if maximum is much larger then end
    * @return
    */

  def stretched(max: Double, stretchMult: Double = 1.1, shrinkMult: Double = -1): FlexibleLinearScale =
  if(max > end) {
    val newEnd = max * stretchMult
    val st = betterStep(Math.abs(newEnd - start) / ticks.length)
    //just a hack to make it look nicer
    this.copy(end = newEnd, stepSize = st)
  }
  else {
    if (shrinkMult > 0 && Math.abs(max - start) > 0.0 && end > max * shrinkMult) {
      val newEnd = end * stretchMult
      val st = betterStep(Math.abs(newEnd - start) / ticks.length)
      this.copy(end = newEnd, stepSize = st)
    } else this //does not change anything
  }


}

trait WithFlexibleScale[T <: Scale] {
  self=>

  def stretched(max: Double, stretchMult: Double = 1.1, shrinkMult: Double = -1): T
}

trait FlexibleLinesPlot extends NewLinesPlot {
  val flexible = Var(true)
  val shrinkMult = Var(1.05)
  val stretchMult = Var(1.05)
  val empty: rx.Rx[Boolean] = items.map(_.isEmpty)
  //def max(series: Series)(fun: Point=>Double): Point = series.points.maxBy(fun)

  val max = items.map{ its =>
    its.foldLeft(Point(0.0, 0.0)){ case (acc, (_, s)) => s.maxOpt match {
      case Some(v) => acc.copy(Math.max(acc.x, v.x), Math.max(acc.y, v.y))
      case _ => acc
    }}
  }

  def onMaxChange(value: Point): Unit





  override def subscribeUpdates() = {
    super.subscribeUpdates()
    max.foreach{onMaxChange}
  }
}
