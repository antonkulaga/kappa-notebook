package org.denigma.controls.charts
import org.denigma.binding.extensions._
import org.scalajs.dom
import rx.Var

import scala.collection.immutable.List
//import rx.Ctx.Owner.voodoo
import rx.Ctx.Owner.Unsafe.Unsafe
/*
case class LinearScaleFixedSteps(title: String, start: Double, end: Double, stepSize: Double, length: Double,
                            optimalSteps: Double,
                            stepSizeFactor: Double = 2,
                            inverted: Boolean = false,
                            precision:Int = 3)
  extends FlexibleLinearScale{


  protected def rescale(newEnd: Double, newStepSize: Double) = {
    val newScale = this.copy(end = newEnd, stepSize = newStepSize)
    if(newScale.ticks.length >
  }

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
*/

case class SimpleFlexibleLinearScale(title: String,
                                     start: Double,
                                     end: Double,
                                     stepSize: Double,
                                     length: Double,
                                     inverted: Boolean = false,
                                     precision: Int = 3,
                                     maxStepsNumber: Int = 20 //if the number of steps is larger than max, then rescale
                                    ) extends FlexibleLinearScale
{

  def stretched(max: Double, stretchMult: Double = 1.1, shrinkMult: Double = -1): FlexibleLinearScale =
    if(max > end) {
      val newEnd = max * stretchMult
      val st = smoothedStep(Math.abs(newEnd - start) / Math.min(ticks.length, maxStepsNumber))
      //just a hack to make it look nicer
      this.copy(end = newEnd, stepSize = st)
    }
    else {
      if (shrinkMult > 0 && Math.abs(max - start) > 0.0 && end > max * shrinkMult) {
        val newEnd = max * stretchMult //TODO: check if it usable
        val st = smoothedStep(Math.abs(newEnd - start) / Math.min(ticks.length, maxStepsNumber))
        this.copy(end = newEnd, stepSize = st)
      }
      else this //does not change anything
    }
}

trait FlexibleLinearScale
  extends WithLinearScale with WithFlexibleScale[FlexibleLinearScale]
{
  def precision: Int

//(title: String, start: Double, end: Double, stepSize: Double, length: Double, inverted: Boolean = false, precision:Int = 3
  private lazy val currentLengh = Math.abs(start - end)

  if(stepSize > Math.max(currentLengh, Math.round(currentLengh))) {
    dom.console.error(s"stepSize(${stepSize}) is larger than the currentLength(${currentLengh})")
  }

  override def points(current: Double, end: Double, dots: List[Double] = List.empty): List[Double]  = {
    val tick = step(current)
    if (current<end) points(truncateAt(tick, precision), end, current::dots) else dots.reverse
  }

  protected def smoothedStep(st: Double): Double = if (st > 1) Math.round(st) else st

  /**
    *
    * @param max maximum value of the point coordinate
    * @param stretchMult makes end strechMult times more then maximum value
    * @param shrinkMult shrinks the scale if maximum is much larger then end
    * @return
    */

  def stretched(max: Double, stretchMult: Double = 1.1, shrinkMult: Double = -1): FlexibleLinearScale

  override def coord(value: Double): Double = if(inverted) inverse(value) * scale else (value - start) * scale //TODO: move this fix to the parent

}

/**
  * Trait for the scale that can stretch itself
  * @tparam T
  */
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
