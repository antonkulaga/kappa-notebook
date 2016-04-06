package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{GeneralBinder, ReactiveBinder}
import org.denigma.binding.macroses._
import org.denigma.binding.views._
import org.denigma.controls.charts.Series
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.tabs._
import org.denigma.kappa.notebook.KappaHub
import org.denigma.kappa.notebook.views.charts.PlotsView
import org.denigma.kappa.notebook.views.editor.{EditorUpdates, SBOLEditor}
import org.denigma.kappa.notebook.views.papers.PapersView
import org.scalajs.dom._
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.collection.immutable.Map


class TabsView(val elem: Element, hub: KappaHub) extends BindableView {

  self =>

  protected def defaultContent = ""
  protected def defaultLabel = ""

  type Item = Rx[TabItem]

  val selected: Var[String] = Var("Plots")

  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  override lazy val injector = defaultInjector
    .register("Plots") {
      case (el, params) =>
        new PlotsView(el, selected, hub).withBinder(new CodeBinder(_))
    }
    .register("Console") {
      case (el, params) =>
        new ConsoleView(el, hub.console, selected).withBinder(new CodeBinder(_))
    }
    .register("Papers") {
      case (el, params) =>
        new PapersView(el, selected, hub).withBinder(new CodeBinder(_))
    }
    .register("SBOLEditor") {
      case (el, params) =>
        new SBOLEditor(el, hub, selected, editorsUpdates).withBinder(new CodeBinder(_))
    }
}



/*
/*
As I am lazy to go to scala-js-binding to fix minor error,
so I create a binder here
 */
class FixedBinder[View <: BindableView](view: View, recover: Option[ReactiveBinder] = None)
                                      (implicit
                                       mpMap: MapRxMap[View], mpTag: TagRxMap[View],
                                       mpString: StringRxMap[View], mpBool: BooleanRxMap[View],
                                       mpDouble: DoubleRxMap[View], mpInt: IntRxMap[View],
                                       mpEvent: EventMap[View], mpMouse: MouseEventMap[View],
                                       mpText: TextEventMap[View], mpKey: KeyEventMap[View],
                                       mpUI: UIEventMap[View], mpWheel: WheelEventMap[View], mpFocus: FocusEventMap[View]
                                      ) extends CodeBinder(view, recover)
{

  def elementPartial(el: Element, ats: Map[String, String]): PartialFunction[(String, String), Unit] =
    upPartial(el, ats)
      .orElse(downPartial(el, ats))
      .orElse(visibilityPartial(el))
      .orElse(classPartial(el))
      .orElse(propertyPartial(el))
      .orElse(setOnPartial(el))
      .orElse(eventsPartial(el))

  override def elementPartial(el: Element, ats: Map[String, String]) = super.elementPartial(el, ats).orElse(codePartial(el, ats))

  protected def mouseEventsPartial(el: Element): PartialFunction[(String, String), Unit] = {
    case (key, value) if mouseEventFromKey.isDefinedAt(key) =>
      val event: String = mouseEventFromKey(key)
      this.bindMapItem(el, mouseEvents, key, value)((e, v) =>
        e.addEventListener[MouseEvent](event, {ev: MouseEvent=>v()= ev })
      )
  }


  override def eventsPartial(el: Element): PartialFunction[(String, String), Unit] = keyboardEventsPartial(el).orElse(mouseEventsPartial(el)).orElse(otherEventsPartial(el))

}
*/