package org.denigma.kappa.notebook.views

import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessages
import org.denigma.kappa.messages.KappaMessages.RunParameters
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLTextAreaElement, HTMLInputElement, Element}
import rx._
import org.denigma.binding.binders.{Events, ReactiveBinder}
import org.denigma.binding.macroses._
import rx.Ctx.Owner.Unsafe.Unsafe

class RunnerView(val elem: Element, val parameters: Var[KappaMessages.RunParameters]) extends BindableView
{
  self=>

  def optInt(n: Int): Option[Int] = if(n > 0.0) Some(n) else None
  def opt(n: Double): Option[Double] = if(n > 0.0) Some(n) else None

  val events: Var[Int] = Var(10000)
  var time: Var[Double] = Var(0.0)
  val points: Var[Int] = Var(250)
  val fileName = Var("model.ka")
  val implicitSignature = Var(true)
  val gluttony: Var[Boolean] = Var(false)

  val output: Rx[Unit] = Rx{
    val fn: String = fileName()
    val ev = self.events()
    val t: Double = self.time()
    val p = self.points()
    val s: Boolean = self.implicitSignature()
    val g = self.gluttony()
    //println(s"params = g($g) and s($s)")
    val newParams: RunParameters = parameters.now.copy( fn, optInt(ev), opt(t), p, implicitSignature = s, gluttony = g )
    parameters.set(newParams)
  }

}


import org.scalajs.dom.ext._


/*
class AdvancedBinder[View <: BindableView](view: View, recover: Option[ReactiveBinder] = None)
                    (implicit
                     mpMap: MapRxMap[View], mpTag: TagRxMap[View],
                     mpString: StringRxMap[View], mpBool: BooleanRxMap[View],
                     mpDouble: DoubleRxMap[View], mpInt: IntRxMap[View],
                     mpEvent: EventMap[View], mpMouse: MouseEventMap[View],
                     mpText: TextEventMap[View], mpKey: KeyEventMap[View],
                     mpUI: UIEventMap[View], mpWheel: WheelEventMap[View], mpFocus: FocusEventMap[View]
                    )
  extends CodeBinder[View](view, recover)(
    mpMap, mpTag,
    mpString, mpBool,
    mpDouble, mpInt,
    mpEvent, mpMouse,
    mpText, mpKey,
    mpUI, mpWheel, mpFocus)
{
  override def bind(el: Element, rxName: String): Unit =  el match
  {
    case inp: HTMLInputElement=>
      el.attributes.get("type").map(_.value.toString) match {
        case Some("checkbox") =>
          //println(s"checked with $rxName")
          this.bindProperty(el, rxName, "checked")
        case _ =>
          subscribeInputValue(el, rxName, Events.keyup, strings)
            .orElse(subscribeInputValue(el, rxName, Events.keyup, doubles))
            .orElse(subscribeInputValue(el, rxName, Events.keyup, ints))
            .orElse(subscribeInputValue(el, rxName, Events.keyup, bools))
            .orError(s"cannot find ${rxName} in ${allValues}")
      }
    case area: HTMLTextAreaElement =>
      subscribeInputValue(el, rxName, Events.keyup, strings)
        .orElse(subscribeInputValue(el, rxName, Events.keyup, doubles))
        .orElse(subscribeInputValue(el, rxName, Events.keyup, ints))
        .orElse(subscribeInputValue(el, rxName, Events.keyup, bools))
        .orError(s"cannot find ${rxName} in ${allValues}")

    case _ =>
      val prop = "textContent" // "innerHTML"
      strings.get(rxName) match {
        case Some(value) =>
          propertyOnRx(el, prop, value)
          varOnEvent[String, Event](el, prop, value, Events.change)
        case None => bindProperty(el, rxName, prop)
      }
  }
}
*/