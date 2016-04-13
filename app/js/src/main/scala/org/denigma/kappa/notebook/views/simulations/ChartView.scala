package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.views.{BindableView, ItemsSeqView}
import org.denigma.controls.charts._
import org.scalajs.dom
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.{Element, SVGElement}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._


class ChartView(val elem: Element,
                val title: Rx[String],
                val items: Rx[Seq[Rx[Series]]]
               ) extends FlexibleLinearPlot {

  protected def defaultWidth = Math.max(dom.window.innerWidth / 2.5, 300)

  protected def defaultHeight = Math.max(dom.window.innerHeight / 2, 400)

  val scaleX: Var[LinearScale] = Var(LinearScale("Time", 0.0, 10, 2, defaultWidth))

  val scaleY: Var[LinearScale] = Var(LinearScale("Concentration", 0.0, 10, 2, defaultHeight, inverted = true))

  val halfWidth = Rx{ width() / 2.0 }

  override def max(series: Series)(fun: Point=>Double): Point = if(series.points.nonEmpty) series.points.maxBy(fun) else Point(0.0, 0.0)

  override val max: rx.Rx[Point] = items.map{case its=>
    val x = its.foldLeft(0.0){ case (acc, series)=> Math.max(acc, if(series.now.points.nonEmpty) series.now.points.maxBy(_.x).x else 0.0)}
    val y = its.foldLeft(0.0){ case (acc, series)=> Math.max(acc, if(series.now.points.nonEmpty) series.now.points.maxBy(_.y).y else 0.0)}
    Point(x, y)
  }

  override def newItemView(item: Item): SeriesView = constructItemView(item){
    case (el, mp) => new SeriesView(el, item, transform).withBinder(new GeneralBinder(_))
  }

  def getFirst[T](pf: PartialFunction[Element,T]): Option[T] = pf.lift(elem).orElse(elem.children.collectFirst(pf))

  import org.denigma.binding.extensions._
  val saveChart = Var(Events.createMouseEvent())
  saveChart.triggerLater{
    getFirst[String]{ case svg: SVGElement => svg.outerHTML} match {
      case Some(html)=>  saveAs(title.now+".svg", html)
      case None=> dom.console.error("cannot find svg element among childrens") //note: buggy
    }
  }

  override lazy val injector = defaultInjector
    .register("ox"){case (el, args) => new AxisView(el, scaleX, chartStyles.map(_.scaleX))
      .withBinder(new GeneralBinder(_))}
    .register("oy"){case (el, args) => new AxisView(el, scaleY, chartStyles.map(_.scaleY))
      .withBinder(new GeneralBinder(_))}
    .register("legend"){case (el, args) => new LegendView(el, items)
      .withBinder(new GeneralBinder(_))}

}
