package org.denigma.kappa.notebook.views.common

import org.scalajs.dom._

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSName, ScalaJSDefined}

object PopStateEventExt {
  implicit def fromPop(ev: PopStateEvent): PopStateEventExt = {
    new PopStateEventExt(ev.`type`, ev.state, ev.bubbles)
  }
}

@js.native
@JSName("PopStateEvent")
class PopStateEventExt(override val `type`: String = js.native, val state: js.Any = js.native,  bubbles: Boolean = false) extends Event {

  def initPopStateEvent(typeArg: String, canBubbleArg: Boolean,
                        cancelableArg: Boolean, stateArg: js.Any): Unit = js.native
}