package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{Events, GeneralBinder, ReactiveBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.macroses._
import org.denigma.binding.views._
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.login.Session
import org.denigma.controls.tabs._
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.Element
import rx.core._

import scala.collection.immutable._
/**
  * Created by antonkulaga on 12/5/15.
  */
class ResultsBinder[View<: BindableView](view: View, recover: Option[ReactiveBinder] = None)
                                        (implicit
                                         mpMap: MapRxMap[View], mpTag:TagRxMap[View],
                                         mpString: StringRxMap[View],  mpBool: BooleanRxMap[View],
                                         mpDouble: DoubleRxMap[View], mpInt: IntRxMap[View],
                                         mpEvent: EventMap[View],  mpMouse: MouseEventMap[View],
                                         mpText: TextEventMap[View], mpKey: KeyEventMap[View],
                                         mpUI: UIEventMap[View], mpWheel: WheelEventMap[View], mpFocus: FocusEventMap[View]
                                        )
  extends CodeBinder(view, recover) {

  override def elementPartial(el: Element, ats: Map[String, String]) =
    super.elementPartial(el,ats).orElse(setOnPartial(el)).orElse(codePartial(el,ats))

  protected def noDash(key: String) = key.replace("-", "")

  protected def keyboardEventFromKey: PartialFunction[String, String] = {
    case key if noDash(key).contains(Events.keyup) => Events.keyup
    case key if noDash(key).contains(Events.keydown) => Events.keydown
    case key if noDash(key).contains(Events.keypress) => Events.keypress
  }

  protected def mouseEventFromKey: PartialFunction[String, String] = {
    case key if noDash(key).contains(Events.mouseenter) => Events.mouseenter
    case key if noDash(key).contains(Events.mouseleave) => Events.mouseleave
    case key if noDash(key).contains(Events.mouseup) => Events.mouseup
    case key if noDash(key).contains(Events.mousedown) => Events.mousedown
    case key if noDash(key).contains(Events.click) => Events.click
    case key if noDash(key).contains(Events.mouseover) => Events.mouseover
    case key if noDash(key).contains(Events.mouseout) => Events.mouseout
  }


  private val SET = "set-"
  private val ON = "-on-"

  /**
    * Changes value of some variable on some event
    * an example:
    * <a class="active item" data-set-tab-on-click="tab">Console</a>
    * @return
    */
  def setOnPartial(el: Element): PartialFunction[(String, String), Unit] = {
    case (key, value) if key.startsWith(SET) && key.contains(ON) =>
        println(s"KEY works: $key")
        mouseEventFromKey.orElse(keyboardEventFromKey).lift(key) match {
          case Some(event) =>
            val (from: Int, to: Int)  = (key.indexOf(SET), key.indexOf(ON))
            if(from > -1 && to > from) {
            val where = key.substring(from, to)
            strings.get(where) match {
              case Some(vstr: Var[String]) => el.addEventListener[Event](event,{
                ev: Event =>
                  println(s"${vstr.now} = $value")
                  vstr()= value
              })
              case _ =>
              println(s"WHERE = $where")
              //el.addEventListener[MouseEvent](event,{ev => v()=ev})
            }
          }
          else dom.console.error(s"settings expression is wrong: $key")
        case None => dom.console.error(s"cannot find event in key =  $key with value =  $value")
      }
  }


}
