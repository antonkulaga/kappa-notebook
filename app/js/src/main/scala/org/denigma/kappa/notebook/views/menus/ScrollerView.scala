package org.denigma.kappa.notebook.views.menus

import org.denigma.binding.binders.Events
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.kappa.messages.{Go, KappaMessage}
import org.denigma.kappa.notebook.views.common.FixedPopStateEvent
import org.denigma.malihu.scrollbar.JQueryScrollbar._
import org.denigma.malihu.scrollbar._
import org.querki.jquery.$
import org.scalajs.dom
import org.scalajs.dom.raw.{Element, PopStateEvent}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

class ScrollerView(val elem: Element,
                   scrollPanel: Element,
                   input: Rx[KappaMessage],
                   menuMap: Rx[Map[String, Element]]
                  ) extends BindableView{

  lazy val scroller: JQueryScrollbar = initScroller()

  val backClick = Var(Events.createMouseEvent())
  backClick.onChange{
    case ev=>
      scrollHistory.now match {
        case Nil =>
          dom.console.error("back should be invisible when there is not history")
        //do nothing

        case head::Nil =>
          scrollHistory() = Nil
          dom.window.history.back()
          scrollPanel.scrollLeft = head.currentPosition

        case head::tail =>
          scrollHistory() = tail
          dom.window.history.back()
      }
  }

  val forwardClick = Var(Events.createMouseEvent())
  forwardClick.onChange{ ev=> dom.window.history.back()}
  val scrollHistory: Var[List[ScrollPosition]] = Var(List.empty[ScrollPosition])

  val hasHistory = scrollHistory.map(v=>v.nonEmpty)

  protected def historyState: Option[ScrollPosition] = dom.window.history.state match {
    case some if js.isUndefined(some) => None
    case right if !js.isUndefined(right.dyn.index)=>Some(right.asInstanceOf[ScrollPosition])
    case other => None
  }

  protected def moveToTab(tab: String) = menuMap.now.get(tab) match {
    case Some(target) =>
      val tid = target.id
      val hash =  "#"+tid
      if(dom.window.location.hash==hash) {
        dom.window.location.hash = ""
      }
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
        //val left = element.offsetLeft
        //scrollPanel.scrollLeft = left
        scroller.scrollTo("#"+ident)
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
    initScroller()
  }

  protected def initScroller(): JQueryScrollbar = {
    val params = new mCustomScrollbarParams(axis = "x", advanced = new mCustomScrollbarAdvancedParams(true), mouseWheel = new MouseWheel(false))
    $(scrollPanel).mCustomScrollbar(params)
  }

  protected def onMessage(message: KappaMessage): Unit = message match {

    case Go.ToTab(tabName) => moveToTab(tabName)

    case other => //do nothing
  }

}



@ScalaJSDefined
class ScrollPosition(val id: String, val currentPosition: Double) extends js.Object {

}