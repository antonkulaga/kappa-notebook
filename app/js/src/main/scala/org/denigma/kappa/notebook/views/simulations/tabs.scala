package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.commons.Uploader
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsSetView}
import org.denigma.controls.charts._
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.WebSim.{RunModel, SimulationStatus}
import org.denigma.kappa.messages.{KappaChart, KappaSeries}
import org.denigma.kappa.notebook.KappaHub
import org.denigma.kappa.notebook.views.common._
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.{Element, Event, SVGElement}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.List
import scala.Predef.{Map, Set}
import scala.collection.immutable._
import scala.util._

class TabHeaders(val elem: Element, val items: Rx[SortedSet[String]], val selected: Var[String]) extends ItemsSetView {


  override type Item =  String

  override type ItemView = TabHeaderItemView

  override def newItemView(item: Item): TabHeaderItemView = constructItemView(item){
    case (el, _) => new TabHeaderItemView(el, item,  selected).withBinder(new GeneralBinder(_))
  }
}

class TabHeaderItemView(val elem: Element, viewId: String,  val selected: Var[String] ) extends BindableView {

  val caption: Var[String] = Var(viewId)

  val active: rx.Rx[Boolean] = selected.map(value => value == viewId)

  val select = Var(Events.createMouseEvent())
  select.triggerLater({
    selected() = viewId
    }
  )
}
