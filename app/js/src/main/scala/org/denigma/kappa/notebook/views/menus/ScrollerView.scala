package org.denigma.kappa.notebook.views.menus

import org.denigma.binding.binders.Events
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.kappa.messages.{Go, KappaMessage}
import org.denigma.kappa.notebook.views.common.FixedPopStateEvent
import org.scalajs.dom
import org.scalajs.dom.raw.{Element, HTMLElement, PopStateEvent}
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.ScalaJSDefined

class ScrollerView(val elem: Element,
                   scrollPanel: Element,
                   input: Rx[KappaMessage],
                   menuMap: Rx[Map[String, Element]]
                  ) extends BindableView{

  val backClick = Var(Events.createMouseEvent())
  backClick.onChange{
    case ev=>
      scrollHistory.now match {
        case Nil =>
          dom.console.error("back should be invisible when there is not history")
        //do nothing

        case head::Nil =>
          scrollHistory() = Nil
          scrollPanel.scrollLeft = head.currentPosition

        case head::tail =>
          scrollHistory() = tail
          dom.window.history.back()
      }
  }

  val forwardClick = Var(Events.createMouseEvent())
  forwardClick.onChange{
    case ev=>
      //dom.window.history.back()
  }
  val scrollHistory: Var[List[ScrollPosition]] = Var(List.empty[ScrollPosition])

  val hasHistory = scrollHistory.map(v=>v.length > 1)

  protected def historyState: Option[ScrollPosition] = dom.window.history.state match {
    case some if js.isUndefined(some) => None
    case right if !js.isUndefined(right.dyn.index)=>Some(right.asInstanceOf[ScrollPosition])
    case other => None
  }

  protected def moveToTab(tab: String) = menuMap.now.get(tab) match {
    case Some(target) =>
      val tid = target.id
      //val index = historyState.map(v=>v.index).getOrElse(0) + 1
      val stateObject = new ScrollPosition(tid, scrollPanel.scrollLeft)
      dom.window.history.pushState(stateObject, tab, "#"+tid)
      val state = js.Dynamic.literal(
        bubbles = false,
        cancelable = false,
        state = stateObject
      )
      var popStateEvent = new FixedPopStateEvent("popstate", state)
      scrollHistory() = stateObject::scrollHistory.now
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



@ScalaJSDefined
class ScrollPosition(val id: String, val currentPosition: Double) extends js.Object {

}