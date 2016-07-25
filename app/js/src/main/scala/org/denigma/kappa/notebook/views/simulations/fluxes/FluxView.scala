package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.binding.views.{BindableView, UpdatableView}
import org.denigma.kappa.messages.WebSimMessages.FluxMap
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._

class FluxView(val elem: Element, val name: String, val item: Var[FluxMap], val tab: Rx[String]) extends BindableView with UpdatableView[FluxMap] {

  lazy val container = elem.selectByClass("graph")

  val code = item.map(i=>i.toString)

  val active = tab.map(t=>t==name)

  override def update(value: FluxMap)= {
    item()  = value
    this
  }
}
