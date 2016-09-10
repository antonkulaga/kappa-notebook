package org.denigma.malihu.scrollbar
import org.querki.jquery.JQuery
import org.querki.jsext._
import org.scalajs.dom.raw.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

object JQueryScrollbar {
  implicit def jq2Scrollbar(jq: JQuery): JQueryScrollbar =
    jq.asInstanceOf[JQueryScrollbar]

  implicit class ExtendedScrollbar(jq: JQuery)  {
    def scrollTo(selector: String): Any = {
      jq.mCustomScrollbar("scrollTo", selector)
    }

    def scrollTo(element: Element): Any = {
      jq.mCustomScrollbar("scrollTo", element)
    }

  }
}

@js.native
trait JQueryScrollbar extends JQuery {
  def mCustomScrollbar(params: mCustomScrollbarParams): JQueryScrollbar = js.native

  def mCustomScrollbar(to: String, selector: String): JQueryScrollbar = js.native

  def mCustomScrollbar(to: String, element: Element): JQueryScrollbar = js.native

}

@js.native
trait mCustomScrollbarParams extends js.Object
object mCustomScrollbarParams extends mCustomScrollbarParamsBuilder(noOpts)
class mCustomScrollbarParamsBuilder(val dict: OptMap) extends JSOptionBuilder[mCustomScrollbarParams, mCustomScrollbarParamsBuilder](new mCustomScrollbarParamsBuilder(_)) {
  def axis(value: String) = jsOpt("axis", value)
  def theme(value: String) = jsOpt("theme", value)
  def advanced(value: mCustomScrollbarAdvancedParams) = jsOpt("advanced", value)
  def mouseWheel(value: MouseWheel) = jsOpt("mouseWheel", value)
  def callbacks(value: ScrollbarCallbacks) = jsOpt("callbacks", value)
}

@js.native
trait ScrollbarCallbacks extends js.Object
object ScrollbarCallbacks extends  ScrollbarCallbacksBuilder(noOpts)
class ScrollbarCallbacksBuilder(val dict: OptMap) extends JSOptionBuilder[ScrollbarCallbacks, ScrollbarCallbacksBuilder](new ScrollbarCallbacksBuilder(_))
{
  def onScrollStart(value: js.Function0[_]): ScrollbarCallbacksBuilder = jsOpt("onScrollStart", value)
  def setOnScrollStart(fun: ()=> Unit) = onScrollStart(fun)
  def onScroll(value: js.Function0[_]): ScrollbarCallbacksBuilder = jsOpt("onScroll", value)
  def setOnScroll(fun: ()=> Unit) = onScroll(fun)
  def whileScrolling(value: js.Function0[_]) = jsOpt("whileScrolling", value)
  def setWhileScrolling(fun: ()=>Unit) = whileScrolling(fun)
  def onTotalScroll(value: js.Function0[_]) = jsOpt("onTotalScroll", value)
  def setOnTotalScroll(fun: ()=> Unit) = onTotalScroll(fun)
  def onTotalScrollBack(value: js.Function0[_]) = jsOpt("onTotalScrollBack", value)
  def setOnTotalScrollBack(fun: ()=>_) = onTotalScrollBack(fun)
  def onOverflowX(value: js.Function0[_]) = jsOpt("onOverflowX", value)
  def setOnOverflowX(fun: ()=>_) = onOverflowX(fun)
  def onOverflowY(value: js.Function0[_]) = jsOpt("onOverflowY", value)
  def setOnOverflowY(fun: ()=>_) = onOverflowY(fun)

  def onOverflowXNone(value: js.Function0[_]) = jsOpt("onOverflowXNone", value)
  def setOnOverflowXNone(fun: ()=>_) = onOverflowXNone(fun)
  def onOverflowYNone(value: js.Function0[_]) = jsOpt("onOverflowYNone", value)
  def setOnOverflowYNone(fun: ()=>_) = onOverflowYNone(fun)


  def onBeforeUpdate(value: js.Function0[_]) = jsOpt("onBeforeUpdate", value)
  def setOnBeforeUpdate(fun: ()=>_) = onBeforeUpdate(fun)
  def onUpdate(value: js.Function0[_]) = jsOpt("onUpdate", value)
  def setOnUpdate(fun: ()=> Unit) = onUpdate(fun)

}
/*
@js.native
trait mCustomScrollbarAdvancedParams extends js.Object
object mCustomScrollbarAdvancedParams
class mCustomScrollbarAdvancedParamsBuilder
*/

@ScalaJSDefined
class mCustomScrollbarAdvancedParams(val autoExpandHorizontalScroll: Boolean,
                                     val updateOnContentResize: Boolean = true,
                                     val updateOnBrowserResize:Boolean = true
                                    ) extends js.Object{

}
/*
object mCustomScrollbarCallbacks {

  def apply(onOverflowX: () => _, onOverflowY: () => _) = new mCustomScrollbarCallbacks(onOverflowX, onOverflowY)
}

@ScalaJSDefined
class mCustomScrollbarCallbacks(val  onOverflowX: js.Function0[_], val onOverflowY: js.Function0[_] ) extends js.Object{

}
*/

package object scrollbar {
  implicit def jq2Scrollbar(jq: JQuery): JQueryScrollbar =
    jq.asInstanceOf[JQueryScrollbar]
}


@ScalaJSDefined
class MouseWheel(val enable: Boolean = true,
                  val disableOver: js.Array[String] = js.Array("select","option","keygen","datalist","textarea")) extends js.Object {

}