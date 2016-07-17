package org.denigma.kappa.notebook.views.common

import org.denigma.binding.binders.ReactiveBinder
import org.denigma.binding.macroses.{DragEventMap, FocusEventMap, UIEventMap, WheelEventMap, _}
import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx.Rx

import scala.scalajs.js
import scala.util.{Failure, Try}
import org.denigma.binding.extensions._
import rx.Ctx.Owner.Unsafe.Unsafe

/**
  * Created by antonkulaga on 7/17/16.
  */
class FixedBinder[View<: BindableView](view: View, recover: Option[ReactiveBinder] = None)
                                      (implicit
                  mpMap: MapRxMap[View], mpTag: TagRxMap[View],
                  mpString: StringRxMap[View], mpBool: BooleanRxMap[View],
                  mpDouble: DoubleRxMap[View], mpInt: IntRxMap[View],
                  mpEvent: EventMap[View], mpMouse: MouseEventMap[View],
                  mpText: TextEventMap[View], mpKey: KeyEventMap[View],
                  mpUI: UIEventMap[View], mpWheel: WheelEventMap[View],
                  mpFocus: FocusEventMap[View],  mpDrag: DragEventMap[View]
                 ) extends CodeBinder[View](view, recover)(

  mpMap, mpTag,
  mpString, mpBool,
  mpDouble, mpInt,
  mpEvent, mpMouse,
  mpText, mpKey,
  mpUI, mpWheel, mpFocus, mpDrag) {

  protected lazy val specialAttributes = Map("viewbox"->"viewBox", "preserveaspectratio"-> "preserveAspectRatio")

  protected def setAttribute[T](e: Element, prop: String, value: T)(implicit conv: T => js.Any) = {
    //println(s"set attribute $prop with value $value")
    val property = specialAttributes.getOrElse(prop, prop) //fir for nonLowerCaseAttributes in SVG
    e.setAttribute(property, value.toString)
    Try(e.dyn.updateDynamic(property)(value)) match {
      case Failure(th) =>
        if(warnOnUpdateFailures)
          dom.console.warn(s"cannot set $prop to $value because of ${th.getMessage} with stack ${th.stackString} \nIN: ${e.outerHTML}")
      case _=>
    }
  }

  override protected def propertyOnRx[T](el: Element, prop: String, value: Rx[T])(implicit conv: T => js.Any): Unit =
  {
    value.foreach{case v => setAttribute(el, prop, v.toString)}
  }
}
