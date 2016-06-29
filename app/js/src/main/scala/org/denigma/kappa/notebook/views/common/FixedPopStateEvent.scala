package org.denigma.kappa.notebook.views.common

import org.scalajs.dom._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Created by antonkulaga on 29/06/16.
  */
@js.native
@JSName("PopStateEvent")
class FixedPopStateEvent(val typeArg: String, override val state: js.Any) extends PopStateEvent
{

}
