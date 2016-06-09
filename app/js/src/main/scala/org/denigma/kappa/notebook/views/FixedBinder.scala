package org.denigma.kappa.notebook.views

import org.denigma.binding.binders._
import org.denigma.binding.views.{BindableView, ItemsSeqView}
import org.scalajs.dom._
import org.scalajs.dom.raw.{HTMLElement, Element, SVGElement}
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.macroses._
import org.denigma.binding.extensions._

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
  extends GeneralBinder[View](view, recover)(
    mpMap, mpTag,
    mpString, mpBool,
    mpDouble, mpInt,
    mpEvent, mpMouse,
    mpText, mpKey,
    mpUI, mpWheel, mpFocus, mpDrag)
{

  //println("DRAG EVENTS = "+dragEvents.keySet)

  override protected def dragFromKey: PartialFunction[String, String] = {
    case key if noDash(key).contains(Events.beforecopy) => Events.beforecopy
    case key if noDash(key).contains(Events.beforecut) => Events.beforecut
    case key if noDash(key).contains(Events.beforepaste) => Events.beforepaste
    case key if noDash(key).contains(Events.copy) => Events.copy
    case key if noDash(key).contains(Events.cut) => Events.cut
    case key if noDash(key).contains(Events.paste) => Events.paste
    case key if noDash(key).contains(Events.dragend) => Events.dragend
    case key if noDash(key).contains(Events.dragenter) => Events.dragenter
    case key if noDash(key).contains(Events.dragleave) => Events.dragleave
    case key if noDash(key).contains(Events.dragover) => Events.dragover
    case key if noDash(key).contains(Events.dragstart) => Events.dragstart
    case key if noDash(key).contains(Events.drag) => Events.drag
    case key if noDash(key).contains(Events.drop) => Events.drop
  }

  override protected def dragEventsPartial(el: Element): PartialFunction[(String, String), Unit] = {
    case (key, value) if dragFromKey.isDefinedAt(key) =>
      val event: String = dragFromKey(key)
      this.bindMapItem(el, dragEvents, key, value)((e, v) =>
        e.addEventListener[DragEvent](event, {ev: DragEvent=>v()= ev })
      )
  }
  override def eventsPartial(el: Element): PartialFunction[(String, String), Unit] = keyboardEventsPartial(el)
    .orElse(mouseEventsPartial(el))
    .orElse(wheelEventsPartial(el))
    .orElse(dragEventsPartial(el))
    .orElse(focusEventsPartial(el))
    .orElse(otherEventsPartial(el))

  override def elementPartial(el: Element, ats: Map[String, String]): PartialFunction[(String, String), Unit] =
    upPartial(el, ats)
      .orElse(downPartial(el, ats))
      .orElse(visibilityPartial(el))
      .orElse(classPartial(el))
      .orElse(propertyPartial(el))
      .orElse(setOnPartial(el))
      .orElse(eventsPartial(el))
}
