package org.denigma.controls.charts

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, CollectionSeqView, CollectionSortedMapView}
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.collection.immutable._

class PlotSeriesView(elem: Element, val series: Var[PlotSeries],
                     transform: Rx[Point => Point],
                     threshold:Point = Point(1,1),
                     closed: Boolean = false)
  extends SeriesView(elem, series, transform, threshold, closed ) {
}

/**
  * View to display legend information
  * @param elem html element to bind to
  * @param items sortedmap of plot data series
  */
class PlotLegendView(val elem: Element, val items: rx.Rx[SortedMap[String, PlotSeries]]) extends CollectionSortedMapView with BindableView{

  type Key = String
  type Value = PlotSeries
  type ItemView = PlotLegendItemView

  override def updateView(view: PlotLegendItemView, key: String, old: PlotSeries, current: PlotSeries): Unit = {
    view.series() = current
  }

  override def newItemView(key: String, value: Value): PlotLegendItemView = this.constructItemView(key) {
    case (el, _) => new PlotLegendItemView(el, Var(value)).withBinder(v=> new GeneralBinder(v))
  }
}

class PlotLegendItemView(val elem: Element, val series: Var[Series]) extends BindableView
{
  val color = series.map(s => s.style.strokeColor)
  val title = series.map(s => s.title)

}

/*
class PlotAxisView(val elem: Element, scale: Rx[Scale], style: Rx[LineStyles])
  extends BindableView with CollectionSeqView
{

  override type Item = Var[Tick]
  override type ItemView = TickView

  val title = scale.map(_.title)
  //val start: rx.Rx[Double] = scale.map(_.start)
  //val end: rx.Rx[Double] = scale.map(_.end)

  val startCoord = scale.map(_.startCoord)
  val endCoord = scale.map(_.endCoord)
  val length = Rx{scale().length}
  val ticks = scale.map(_.ticks)
  //val inverted = scale.map(_.inverted)

  val strokeWidth = style.map(_.strokeWidth)
  val strokeColor = style.map(_.strokeColor)
  lazy val tickLength = Var(10.0)
  lazy val half = length.map(_/2)

  override val items: Rx[Seq[Item]] = Rx{
    val sc = scale()
    val its = ticks()
    its.map{ i=>
      val name = s"$i"
      val value =  sc.coord(i)
      val tick = new Tick(name,value)
      //println(sc.title+s" $tick")
      Var(tick)
    }
  }

  override def newItemView(item: Item): TickView = this.constructItemView(item){
    (e,m) => new TickView(e, item, tickLength, style).withBinder(v => new GeneralBinder(v))
  }

}
*/