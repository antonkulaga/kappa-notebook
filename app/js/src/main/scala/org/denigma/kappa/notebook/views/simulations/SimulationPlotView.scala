package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.controls.charts._
import org.denigma.kappa.messages.KappaSeries
import org.denigma.kappa.messages.WebSimMessages.KappaPlot
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.{Element, SVGElement}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.collection.immutable._

/*
class ChartView(val elem: Element,
               val title: Rx[String],
               val plot: Rx[KappaPlot],
               val maxOpt: Rx[Option[Double]],
               val selected: Var[String]
              ) extends FlexibleLinearPlot{

  val active: rx.Rx[Boolean] = selected.map(value => value == "plot")


  protected def withMax(value: Point): Point = maxOpt.now match {
    case Some(v) if v >= value.x =>  value.copy(x = v)
    case Some(v) =>
      dom.console.error(s"time/events canno be larger than max!")
      value
    case _ => value
  }

  override def onMaxChange(value: Point): Unit = super.onMaxChange(withMax(value))

 protected def defaultWidth: Double = 1000

 protected def defaultHeight: Double = 1000

 val scaleX: Var[LinearScale] = Var(LinearScale("Time", 0.0, 10, 2, defaultWidth))

 val scaleY: Var[LinearScale] = Var(LinearScale("Concentration", 0.0, 10, 2, defaultHeight, inverted = true))

 val viewBox: Dynamic[String] = Rx{
   s"${0} ${0} ${width()  } ${height() }"
 }

 val halfWidth = Rx{ width() / 2.0 }

 val legend = plot.map(p=>p.legend)

 val legendList: Rx[List[(String, Int, LineStyles)]] = legend.map{ l =>
   l.zipWithIndex.map{ case (tlt, i) => (tlt, i, KappaSeries.randomLineStyle())}
 }

 val items: Rx[List[Var[KappaSeries]]] = Rx{
   val p = plot()
   val leg = legendList()
   leg.map { case (tlt, i, style) => Var(KappaSeries(tlt, p.time_series.map(o => Point(o.observation_time, o.observation_values(i))), style))}
 }

 override def max(series: Series)(fun: Point => Double): Point = if(series.points.nonEmpty) series.points.maxBy(fun) else Point(0.0, 0.0)

 override val max: rx.Rx[Point] = items.map{ its=>
   val x = its.foldLeft(0.0){ case (acc, series)=> Math.max(acc, if(series.now.points.nonEmpty) series.now.points.maxBy(_.x).x else 0.0)}
   val y = its.foldLeft(0.0){ case (acc, series)=> Math.max(acc, if(series.now.points.nonEmpty) series.now.points.maxBy(_.y).y else 0.0)}
   Point(x, y)
 }

 override def newItemView(item: Item): SeriesView = constructItemView(item){
   case (el, mp) => new SeriesView(el, item, transform).withBinder(new GeneralBinder(_))
 }

 import org.denigma.binding.extensions._
 val savePlot: Var[MouseEvent] = Var(Events.createMouseEvent())
 savePlot.triggerLater{
   elem.selectByClass("plot") match {
     case null =>
       dom.console.error("chart svg elemement does not exist")

     case svg: SVGElement =>
       saveAs(title.now+".svg", svg.outerHTML)

     case _ =>
       dom.console.error("cannot find chart SVG element")
   }
 }


  val saveCSV: Var[MouseEvent] = Var(Events.createMouseEvent())
  saveCSV.triggerLater{
    val p = plot.now
    val head = p.legend.foldLeft("time"){ case (acc, e) => acc + "," +e} + "\n"
    val body =  p.time_series.foldLeft(""){
      case (acc, s) =>
        acc + s.observation_time+ s.observation_values.foldLeft(""){
          case (a, ss) => a +"," + ss
        } + "\n"
      }.reverse
    val txt = head + body
    saveAs(title.now+".csv", txt)
  }

 override lazy val injector = defaultInjector
   .register("ox"){case (el, args) => new AxisView(el, scaleX, chartStyles.map(_.scaleX))
     .withBinder(new GeneralBinder(_))}
   .register("oy"){case (el, args) => new AxisView(el, scaleY, chartStyles.map(_.scaleY))
     .withBinder(new GeneralBinder(_))}
   .register("legend"){case (el, args) => new LegendView(el, items)
     .withBinder(new GeneralBinder(_))}

}
*/
object SimulationPlotView {


  protected def defaultWidth: Double = 1000

  protected def defaultHeight: Double = 1000
}


class SimulationPlotView(val elem: Element,
                         val title: Rx[String],
                         val plot: Rx[KappaPlot],
                         val selected: Var[String],
                         val scaleX: Var[FlexibleLinearScale] = Var(SimpleFlexibleLinearScale("Time", 0.0, 10, 2, SimulationPlotView.defaultWidth)),
                         val scaleY: Var[FlexibleLinearScale] = Var(SimpleFlexibleLinearScale("Molecules", 0.0, 10, 2, SimulationPlotView.defaultHeight, inverted = true))
               ) extends FlexibleLinesPlot{

  lazy val legend = plot.map(p=>p.legend)

  lazy val legendList: Rx[List[(String, Int, LineStyles)]] = legend.map{ l =>
    l.zipWithIndex.map{ case (tlt, i) => (tlt, i, KappaSeries.randomLineStyle())}
  }

  lazy val items: Rx[SortedMap[String, PlotSeries]] = plot.map{p=>
    val leg = legendList.now
    val tuples = leg.map { case (tlt, i, style) => tlt -> KappaSeries(tlt, p.observables.map(o => Point(o.observation_time, o.observation_values(i))), style) }
    SortedMap(tuples:_*)
  }

  val active: rx.Rx[Boolean] = selected.map(value => value == "plot")

  def onMaxChange(value: Point): Unit = value match {
    case Point(x, y) =>
      if(flexible.now) {
        val (sX, sY) = (scaleX.now, scaleY.now)
        val sh = shrinkMult.now
        val st = stretchMult.now
        scaleX() = sX.stretched(x, stretchMult = st, shrinkMult = sh)
        scaleY() = sY.stretched(y,  stretchMult = st, shrinkMult = sh)
      }
      //println("=======ticks===========")
      //println(scaleX.now.ticks.mkString(" | "))
  }

  val viewBox: Dynamic[String] = Rx{
    s"${0} ${0} ${width() + paddingX() * 2  } ${height() + paddingY() * 2 }"
  }

  val halfWidth = Rx{ width() / 2.0 }


  import org.denigma.binding.extensions._
  val savePlot: Var[MouseEvent] = Var(Events.createMouseEvent())
  savePlot.triggerLater{
    elem.selectByClass("plot") match {
      case null =>
        dom.console.error("chart svg elemement does not exist")

      case svg: SVGElement =>
        saveAs(title.now+".svg", svg.outerHTML)

      case _ =>
        dom.console.error("cannot find chart SVG element")
    }
  }


  val saveCSV: Var[MouseEvent] = Var(Events.createMouseEvent())
  saveCSV.triggerLater{
    val p = plot.now
    val head = p.legend.foldLeft("time"){ case (acc, e) => acc + "," +e} + "\n"
    val body =  p.observables.foldLeft(""){
      case (acc, s) =>  acc + s.observation_time+ s.observation_values.foldLeft(""){ case (a, ss) => a +"," + ss} + "\n"
    }
    val txt = head + body
    saveAs(title.now+".csv", txt)
  }

  override lazy val injector = defaultInjector
    .register("ox"){case (el, args) => new FlexibleAxisView(el, scaleX, chartStyles.map(_.scaleX)).withBinder(new GeneralBinder(_))}
    .register("oy"){case (el, args) => new FlexibleAxisView(el, scaleY, chartStyles.map(_.scaleY)).withBinder(new GeneralBinder(_))}
    .register("legend"){case (el, args) => new PlotLegendView(el, items).withBinder(new GeneralBinder(_))}

  override def newItemView(key: String, value: PlotSeries): PlotSeriesView = this.constructItemView(key){
    case (el, _) =>
      new PlotSeriesView(el, Var(value), transform).withBinder(v=>new GeneralBinder(v))
  }

}
