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
  val shrinkMult = Var(1.0)
  val stretchMult = Var(1.0)

  val active: rx.Rx[Boolean] = selected.map(value => value == this.id)
  val scaleX = Var(LinearScale("Time", 0.0, 10, 2, 400))
  val scaleY = Var(LinearScale("Concentration", 0.0, 10, 2, 500, inverted = true))

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
      }
  }

  override def newItemView(item: Item): SeriesView = constructItemView(item){
    case (el, mp) => new SeriesView(el, item, transform).withBinder(new GeneralBinder(_))
  }
}