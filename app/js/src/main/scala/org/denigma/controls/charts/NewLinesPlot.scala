package org.denigma.controls.charts


/*
import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionMapView, CollectionSeqView, CollectionSortedMapView}
import org.scalajs.dom.Element
import rx._

import scala.collection.immutable.SortedMap
//import rx.Ctx.Owner.voodoo
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.binders.{Events, GeneralBinder}
import rx._
//import rx.Ctx.Owner.voodoo
import rx.Ctx.Owner.Unsafe.Unsafe

trait NewLinesPlot extends CollectionSortedMapView with Plot
{
self=>

type Key = String

type Value = PlotSeries

override type ItemView = PlotSeriesView

val paddingX = Var(50.0)

val paddingY = Var(50.0)

val chartStyles: Rx[ChartStyles] = Var(ChartStyles.default)

lazy val transform: Rx[Point => Point]  = Rx{ p => p.copy(scaleX().coord(p.x), scaleY().coord(p.y)) }

/*
override def newItemView(item: Item): SeriesView = constructItemView(item){
  case (el, mp) => new SeriesView(el, item, transform).withBinder(new GeneralBinder(_))
}
*/

lazy val fill: rx.Rx[String] = chartStyles.map(_.linesStyles.fill)

lazy val linesStyles = chartStyles.map(_.linesStyles)

lazy val chartClick = Var(Events.createMouseEvent())

protected def flexibleScale(scale: LinearScale, max: Double, mult: Double = 1.2): LinearScale = if(max > scale.end) {
  val newEnd = max * mult
  //println(s"maximizing from ${scale.end} to ${newEnd}")
  scale.copy(end = newEnd, stepSize = newEnd / scale.ticks.size)
} else if(scale.end * 2 > max) {
  //println(s"minimizing from ${scale.end} to $max")
  scale.copy(end = scale.end / 2, stepSize = scale.stepSize / 2)
} else scale


override lazy val injector = defaultInjector
  .register("ox"){case (el, args) => new AxisView(el, scaleX, chartStyles.map(_.scaleX))
    .withBinder(new GeneralBinder(_))}
  .register("oy"){case (el, args) => new AxisView(el, scaleY, chartStyles.map(_.scaleY))
    .withBinder(new GeneralBinder(_))}
  .register("legend"){case (el, args) => new PlotLegendView(el, items)
    .withBinder(new GeneralBinder(_))}

override def updateView(view: ItemView, key: String, old: Value, current: Value): Unit = {
  view.series() = current
}

}
*/