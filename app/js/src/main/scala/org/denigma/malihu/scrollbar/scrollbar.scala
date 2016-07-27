package org.denigma.malihu.scrollbar
import org.querki.jquery.{$, JQuery}

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

object JQueryScrollbar {
  implicit def jq2Scrollbar(jq: JQuery): JQueryScrollbar =
    jq.asInstanceOf[JQueryScrollbar]

  implicit class ExtendedScrollbar(jq: JQuery)  {
    def scrollTo(selector: String): Any = {
      jq.mCustomScrollbar("scrollTo", selector)
    }

  }
}

@js.native
trait JQueryScrollbar extends JQuery {
  def mCustomScrollbar(params: mCustomScrollbarParams): JQueryScrollbar = js.native

  def mCustomScrollbar(to: String, selector: String) = js.native

}


@ScalaJSDefined
class mCustomScrollbarParams(val axis: String,
                             //val theme: String,
                             val advanced: mCustomScrollbarAdvancedParams,
                             val mouseWheel: MouseWheel = new MouseWheel()
                             /*val callbacks:  mCustomScrollbarCallbacks = js.native*/) extends js.Object{

}


@ScalaJSDefined
class mCustomScrollbarAdvancedParams(val autoExpandHorizontalScroll: Boolean,
                                     val updateOnContentResize: Boolean = true,
                                     val updateOnBrowserResize:Boolean = true
                                    ) extends js.Object{

}

@ScalaJSDefined
class mCustomScrollbarCallbacks(val  onOverflowX: js.Function0[_], val onOverflowY: js.Function0[_] ) extends js.Object{

}

package object scrollbar {
  implicit def jq2Scrollbar(jq: JQuery): JQueryScrollbar =
    jq.asInstanceOf[JQueryScrollbar]
}


@ScalaJSDefined
class MouseWheel(val enable: Boolean = true,
                  val disableOver: js.Array[String] = js.Array("select","option","keygen","datalist","textarea")) extends js.Object {

}