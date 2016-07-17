package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.binders.{Events, GeneralBinder, ReactiveBinder}
import org.denigma.binding.macroses._
import org.denigma.binding.views.BindableView
import org.denigma.controls.charts._
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaSeries
import org.denigma.kappa.messages.WebSimMessages.KappaPlot
import org.scalajs.dom
import org.scalajs.dom.raw.{Element, SVGElement}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import org.scalajs.dom.ext._
import rx.Rx.Dynamic
import org.denigma.binding.extensions._
import org.denigma.kappa.notebook.views.common.TabItem
import org.scalajs.dom._

import scala.collection.immutable._
import scala.scalajs.js
import scala.util.{Failure, Try}





class ChartView(val elem: Element,
               val title: Rx[String],
               val plot: Rx[KappaPlot],
               val selected: Var[String]
              ) extends FlexibleLinearPlot{

  pprint.pprintln("PLOT VIEW WORK")


  val active: rx.Rx[Boolean] = selected.map(value => value == "plot")

 //protected def defaultWidth: Double = Math.max(dom.window.innerWidth / 2, 400)

 //protected def defaultHeight: Double = Math.max(dom.window.innerHeight / 2, 400)

 protected def defaultWidth: Double = 1000

 protected def defaultHeight: Double = 1000

 val scaleX: Var[LinearScale] = Var(LinearScale("Time", 0.0, 10, 2, defaultWidth))

 val scaleY: Var[LinearScale] = Var(LinearScale("Concentration", 0.0, 10, 2, defaultHeight, inverted = true))

 val viewBox: Dynamic[String] = Rx{
   s"${0} ${0} ${width()  } ${height() }"
 }

 val halfWidth = Rx{ width() / 2.0 }

 val legend = plot.map(p=>p.legend)

 val legendList: Rx[List[(String, Int, LineStyles)]] = legend.map{ case l =>
   //println("legend changed = "+l)
   l.zipWithIndex.map{ case (tlt, i) =>
     (tlt, i, KappaSeries.randomLineStyle())
   }
 }

 val items: Rx[List[Var[KappaSeries]]] = plot.map { case p =>
   //println("items changed")
   legendList.now.map { case (tlt, i, style) =>
     Var(KappaSeries(tlt, p.observables.map(o => Point(o.time, o.values(i))), style))
   }
 }

 override def max(series: Series)(fun: Point => Double): Point = if(series.points.nonEmpty) series.points.maxBy(fun) else Point(0.0, 0.0)

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
