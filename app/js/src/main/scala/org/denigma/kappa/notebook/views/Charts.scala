package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.controls.charts._
import org.denigma.controls.tabs.{TabItemView, TabItem}
import org.scalajs.dom.raw.Element
import rx.core.{Var, Rx}
import rx.ops._
import scala.collection.immutable._

class Charts(val elem: Element,
                   val items: Rx[Seq[Rx[Series]]],
                   val selected: Var[String]
                  ) extends LinesPlot {

  val active: rx.Rx[Boolean] = selected.map(value => value == this.id)
  val scaleX: rx.Var[Scale] = Var(LinearScale("Time", 0.0, 5000, 600, 400))
  val scaleY: rx.Var[Scale] = Var(LinearScale("Concentration", 0.0, 2000, 400, 400, inverted = true))

  override def newItemView(item: Item): SeriesView = constructItemView(item){
    case (el, mp) => new SeriesView(el, item, transform).withBinder(new GeneralBinder(_))
  }
}