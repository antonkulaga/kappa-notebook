package org.denigma.kappa.notebook.actions

import org.denigma.kappa.messages.UIMessage
import org.denigma.kappa.notebook.graph.layouts.ForceLayoutParams

/**
  * Created by antonkulaga on 8/14/16.
  */
object Commands {

  case object SaveAll extends UIMessage

  case class SetLayoutParameters(graph: String, parameters: ForceLayoutParams) extends UIMessage

}

