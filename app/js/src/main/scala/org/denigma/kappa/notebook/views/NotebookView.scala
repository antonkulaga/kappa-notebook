package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{BinderForViews, Binder, ReactiveBinder, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.macroses._
import org.denigma.binding.views._
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.login.Session
import org.denigma.controls.sockets.{WebSocketStorage, WebSocketSubscriber}
import org.denigma.kappa.messages.KappaMessages
import org.denigma.kappa.notebook.{KappaHub, WebSocketConnector}
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.{WebSocket, Element}
import rx.core._

import scala.scalajs.js
import org.denigma.binding.extensions._

import rx.ops._

class NotebookView(val elem: Element, val session: Session) extends BindableView
{
  self =>

  lazy val subscriber = WebSocketSubscriber("notebook", "guest")

  val hub = KappaHub.empty

  val defaultCode =
    """
      |####### ADD YOUR CODE HERE #############
      |
      |#### Signatures
      |
      |#### Rules
      |
      |#### Variables
      |
      |#### Observables
      |
      |#### Initial conditions
      |
      |#### Modifications
    """.stripMargin

  val connector: WebSocketConnector = WebSocketConnector(subscriber, hub)

  val code = Var(defaultCode)

  hub.code.map{
    case Some(c) => c.code
    case None => defaultCode
  }.onChange("hub changes")(value => code() = value)

  val send = Var(org.denigma.binding.binders.Events.createMouseEvent)
  send.handler{
    dom.console.log("sending the code...")
    connector.send(KappaMessages.Code(code.now))
  }

   override lazy val injector = defaultInjector
    .register("results")((el, args) => new ResultsView(el, hub).withBinder(n => new AdvancedBinder(n)))
}

class BinderForViews2[View<:OrganizedView](par: View) extends BinderForViews(par)
{

  override def createView(el: Element, ats: Map[String, String], viewName: String): View#ChildView =     {
    val params = attributesToParams(ats)
    val id: js.UndefOr[String] = el.id
    if(id.isEmpty)
      el.id = if(parent.subviews.keySet.contains(viewName)) viewName + "#" +  math.round(1000000*math.random) else viewName
    val v = parent.inject(viewName, el, params)
    parent.addView(v) //the order is intentional
    v.bindView()
    v
  }
}


class AdvancedBinder[View <: BindableView](view: View, recover: Option[ReactiveBinder] = None)
                                          (implicit
                                           mpMap: MapRxMap[View], mpTag: TagRxMap[View],
                                           mpString: StringRxMap[View], mpBool: BooleanRxMap[View],
                                           mpDouble: DoubleRxMap[View], mpInt: IntRxMap[View],
                                           mpEvent: EventMap[View], mpMouse: MouseEventMap[View],
                                           mpText: TextEventMap[View], mpKey: KeyEventMap[View],
                                           mpUI: UIEventMap[View], mpWheel: WheelEventMap[View], mpFocus: FocusEventMap[View]
                                          )
  extends CodeBinder[View](view, recover)
{
  import scala.concurrent.duration._
  lazy val defaultDelay = 300  millis

  override def elementPartial(el: Element, ats: Map[String, String]): PartialFunction[(String, String), Unit] =
    //downPartial(el, ats).orElse(super.elementPartial(el, ats))
    downPartial(el, ats)
      .orElse(upPartial(el, ats))
      .orElse(visibilityPartial(el))
      .orElse(this.classPartial(el))
      .orElse(this.propertyPartial(el))
      .orElse(this.setOnPartial(el))
      .orElse(this.eventsPartial(el))
      .orElse(codePartial(el, ats))

  override def setOnPartial(el: Element): PartialFunction[(String, String), Unit] = {
    case (key, value) if key.startsWith(SET) && key.contains(ON) =>
      //println(s"KEY works: $key")
      mouseEventFromKey.orElse(keyboardEventFromKey).lift(key) match {
        case Some(event) =>
          val (from: Int, to: Int)  = (key.indexOf(SET)+SET.length, key.indexOf(ON))
          if(from > -1 && to > from) {
            val where = key.substring(from, to)
            strings.get(where) match {
              case Some(vstr: Var[String]) =>
                //println(s"event is $event and str is $where")
                el.addEventListener[Event](event, {
                ev: Event =>
                  println(s"${where}(${vstr.now}) = $value")
                  vstr()= value
              })
              case _ => dom.console.error(s"cannot find $where variable")
              //el.addEventListener[MouseEvent](event,{ev => v()=ev})
            }
          }
          else dom.console.error(s"settings expression is wrong: $key")
        case None => dom.console.error(s"cannot find event in key =  $key with value =  $value")
      }
  }

  def downPartial(el: Element, ats: Map[String, String]): PartialFunction[(String, String), Unit]  = {
    case (bname, rxName) if rxName.contains(".") =>
      val fun: js.Function0[Any] = ()=>{
        downPartialDelayed(el, bname, rxName, ats)
      }
      dom.setTimeout( fun, defaultDelay.toMillis: Double)
  }

  protected def downPartialDelayed(el: Element, bname: String, rxName: String, ats: Map[String, String]) = {
    val ind = rxName.indexOf(".")
    val childName = rxName.substring(0, ind)
    val childRxName = rxName.substring(ind + 1)
    view.subviews.get(childName) match {
      case Some(child)=>
        child.binders.foreach{
          case b: ReactiveBinder =>
            //println(s"DOWN PARTIAL IS ${bname} -> ${childRxName}")
            b.elementPartial(el, ats.updated(bname, childRxName))(bname, childRxName)
          case other: child.type#ViewBinder=> // do nothing
        }

      case None => dom.console.error(s"cannot bind to child view's Rx with Name $childName and RxName ${rxName}\n " +
        s"delay is ${defaultDelay.toMillis}" +
        s"all child views are: [${view.subviews.keySet.toList.mkString(", ")}]" +
        s"")
    }
  }

}


/*

class CodeCellBinder(view: BindableView, onCtrlEnter: Doc => Unit) extends CodeBinder(view) {

  lazy val ctrlHandler: js.Function1[Doc, Unit] = onCtrlEnter
  //lazy val delHandler:js.Function1[Doc,Unit] = onDel


  override def makeEditor(area: HTMLTextAreaElement, textValue: String, codeMode: String, readOnly: Boolean = false) = {
    val editor = super.makeEditor(area, textValue, codeMode, readOnly)
    val dic = js.Dictionary(
      "Ctrl-Enter" -> ctrlHandler
    )
    editor.setOption("extraKeys", dic)
    editor
  }

}
*/
