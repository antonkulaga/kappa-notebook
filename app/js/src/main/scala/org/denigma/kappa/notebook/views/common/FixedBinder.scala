package org.denigma.kappa.notebook.views.common

import org.denigma.binding.binders.{Events, GeneralBinder, ReactiveBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.macroses._
import org.denigma.binding.views.BindableView
import org.denigma.codemirror.extensions.EditorConfig
import org.denigma.codemirror.{CodeMirror, Editor}
import org.denigma.controls.code.CodeBinder
import org.querki.jquery._
import org.scalajs.dom
import org.scalajs.dom.raw._
import rx._
import org.scalajs.dom.ext._
import org.scalajs.dom.{Element => _, Event => _, KeyboardEvent => _, _}

import scala.scalajs.js
import scala.util.{Failure, Success, Try}
//import rx.Ctx.Owner.voodoo
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.collection.immutable.Map
class FixedBinder[View <: BindableView](view: View, recover: Option[ReactiveBinder] = None)
                                       (implicit
                                        mpMap: MapRxMap[View], mpTag: TagRxMap[View],
                                        mpString: StringRxMap[View], mpBool: BooleanRxMap[View],
                                        mpDouble: DoubleRxMap[View], mpInt: IntRxMap[View],
                                        mpEvent: EventMap[View], mpMouse: MouseEventMap[View],
                                        mpText: TextEventMap[View], mpKey: KeyEventMap[View],
                                        mpUI: UIEventMap[View], mpWheel: WheelEventMap[View],
                                        mpFocus: FocusEventMap[View],  mpDrag: DragEventMap[View]
                                       )
  extends CodeBinder[View](view, recover)(
    mpMap, mpTag,
    mpString, mpBool,
    mpDouble, mpInt,
    mpEvent, mpMouse,
    mpText, mpKey,
    mpUI, mpWheel, mpFocus, mpDrag)
{

  override def elementPartial(el: Element, ats: Map[String, String]) =    upPartial(el, ats)
    .orElse(downPartial(el, ats))
    .orElse(visibilityPartial(el))
    .orElse(classPartial(el))
    .orElse(propertyPartial(el))
    .orElse(setOnPartial(el))
    .orElse(eventsPartial(el))
    .orElse(codePartial(el, ats))



  override protected def varOnEvent[T, TEvent <: dom.Event](el: Element, prop: String, value: Rx[T], event: String)
                                                  (implicit js2var: js.Any => T): Unit =
  {
    value.onVar { v =>
      el.addEventListener[TEvent](event,(ev: TEvent) => {
        if(ev.target==ev.currentTarget) el.onExists(prop){ newValue=>
            Try(js2var(newValue)) match {
              case Success(newVal)=>
                v() = newVal
              case Failure(th)=> dom.console.warn(s"cannot convert ${newValue} to Var , failure: ${th}")
            }
          }
        }
      )
      val curV = js2var(el.dyn.selectDynamic(prop))
      v() = curV
    }
  }

  protected def subscribeOnEvent[T, Event <: dom.Event](el: Element, rxName: String, prop: String, event: String, mp: Map[String, Rx[T]])
                                      (implicit js2var: js.Any => T): Option[Rx[T]] =
    mp.get(rxName) map {
      value =>
        //this.bindProperty(el, rxName, prop)
        varOnEvent[T, Event](el, prop, value, event)(js2var)
        //propertyOnRx(el,prop,value)
        value
    }

  protected def bindInput(inp: HTMLInputElement, rxName: String): Unit =
  {
    inp.attributes.get("type").map(_.value.toString) match {
      case Some("checkbox") =>
        subscribeOnEvent(inp, rxName, "checked", Events.change, bools){
          case  any =>
            any.asInstanceOf[Boolean]
        }

      case Some("radio") =>
        subscribeOnEvent(inp, rxName, "checked", Events.change, bools){
          case  any =>
            any.asInstanceOf[Boolean]
        }
        
      case _ =>
        subscribeInputValue(inp, rxName, Events.keyup, strings)
          .orElse(subscribeInputValue(inp, rxName, Events.keyup, doubles))
          .orElse(subscribeInputValue(inp, rxName, Events.keyup, ints))
          .orElse(subscribeInputValue(inp, rxName, Events.keyup, bools))
          .orError(s"cannot find ${rxName} in ${allValues}")
    }
  }
  /**
    * Binds property
    * @param el html element
    * @param rxName name of Rx
    * @return
    */
  override def bind(el: Element, rxName: String): Unit =  el match
  {
    case inp: HTMLInputElement=>
      bindInput(inp, rxName)
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