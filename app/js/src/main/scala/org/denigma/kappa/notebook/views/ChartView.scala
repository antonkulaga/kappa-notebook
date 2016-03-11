package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.commons.Uploader
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.controls.charts._
import org.denigma.kappa.notebook.KappaHub
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.{Element, Event, SVGElement}
import rx.{Rx, Var}
import rx.Ctx.Owner.Unsafe.Unsafe


import scala.collection.immutable._
import scala.util._


class PlotsView(val elem: Element, val selected: Var[String], hub: KappaHub) extends BindableView with Uploader with TabItem
{
  self=>

  val chartActive = Var(true)
  val outputActive = Var(true)

  val output = Var("")
/*
  val applyOutput: Var[MouseEvent] = Var(Events.createMouseEvent())
  applyOutput.triggerLater{
    val text = output.now
    val lines = output.now.split("\n").toVector
    //hub.output() = hub.output.now.copy(lines = lines)
  }
  */
/*
  hub.output.foreach{case h=>
    output.set(h.text)
  }
*/
  val saveOutput: Var[MouseEvent] = Var(Events.createMouseEvent())
  saveOutput.triggerLater{
    saveAs(hub.name.now, output.now)
  }

  val activateChart: Var[MouseEvent] = Var(Events.createMouseEvent())
  activateChart.triggerLater{
    chartActive() = !chartActive.now
  }

  val activateOutput: Var[MouseEvent] = Var(Events.createMouseEvent())
  activateOutput.triggerLater{
    outputActive() = !outputActive.now
  }

  val onUpload: Var[Event] = Var(Events.createEvent())
  onUpload.onChange(ev =>
    this.uploadHandler(ev){
      case Success((file, text))=> output.set(text)
      case Failure(th) => dom.console.error(s"File upload failure: ${th.toString}")
    })

  val title = hub.name.map(_.replace(".out",""))

  override lazy val injector = defaultInjector
    .register("Chart") {
      case (el, params) =>
        val items: rx.Rx[scala.collection.immutable.Seq[Rx[Series]]] =  hub.chart.map(chart=>chart.series.map(s=>Var(s)))
        new ChartView(el, title, items).withBinder(new GeneralBinder(_))
    }

}


class ChartView(val elem: Element,
                val title: Rx[String],
                val items: Rx[Seq[Rx[Series]]]
               ) extends FlexibleLinearPlot {

   val scaleX: Var[LinearScale] = Var(LinearScale("Time", 0.0, 10, 2, 400))
   val scaleY: Var[LinearScale] = Var(LinearScale("Concentration", 0.0, 10, 2, 500, inverted = true))

  val halfWidth = Rx{ width() / 2.0 }

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

}

