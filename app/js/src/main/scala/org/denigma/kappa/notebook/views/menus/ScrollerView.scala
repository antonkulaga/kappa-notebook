package org.denigma.kappa.notebook.views.menus

import org.denigma.binding.binders.Events
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.kappa.messages.{Go, KappaMessage}
import org.denigma.kappa.notebook.views.common.FixedPopStateEvent
import org.scalajs.dom
import org.scalajs.dom.raw.{Element, HTMLElement, PopStateEvent}
import rx._

import scala.scalajs.js
import scala.scalajs.js.JSON

class ScrollerView(val elem: Element,
                   scrollPanel: Element,
                   input: Rx[KappaMessage],
                   menuMap: Rx[Map[String, Element]]
                  ) extends BindableView{

  val backClick = Var(Events.createMouseEvent())
  backClick.onChange{
    case ev=> dom.window.history.back()
  }

  val forwardClick = Var(Events.createMouseEvent())
  forwardClick.onChange{
    case ev=> dom.window.history.back()
  }

  val hasHistory = Var(false)

  protected def moveToTab(tab: String) = menuMap.now.get(tab) match {
    case Some(target) =>
      val tid = target.id
      val stateObject = new ScrollPosition(tid, scrollPanel.scrollLeft)
      dom.window.history.pushState(stateObject, tab, "#"+tid)
      val state = js.Dynamic.literal(
        bubbles = false,
        cancelable = false,
        state = stateObject
      )
      var popStateEvent = new FixedPopStateEvent("popstate", state)
      dom.window.dispatchEvent(popStateEvent)
      //println("pop event dispatched")
    case None =>
      dom.console.error("cannot find tab "+tab)
  }

  protected def scrollTo(ident: String) = {
    sq.byId(ident) match {
      case Some(element)=>
        val left = element.offsetLeft
        scrollPanel.scrollLeft = left
      case None =>
        dom.console.error("cannot scroll to "+ident)
    }
  }

  protected def popStateHandler(ppe: PopStateEvent): Unit = {
    hasHistory() = true
    println("state is: \n "+JSON.stringify(ppe.state))
    ppe.state match {
      case value if js.isUndefined(value) => dom.console.error("scroll to undefined id")
      case null => dom.console.error("scroll to null")
      case pos if !js.isUndefined(pos.dyn.id) =>
        scrollTo(pos.dyn.id.toString)

      case st =>
        val gid = st.toString
        scrollTo(gid)
    }
  }

  override def bindView() = {
    super.bindView()
    dom.window.onpopstate = popStateHandler _
    input.onChange(onMessage)
  }

  protected def onMessage(message: KappaMessage): Unit = message match {

    case Go.ToTab(tabName) => moveToTab(tabName)

    case other => //do nothing
  }

}
