package org.denigma.kappa.notebook.actions

import org.denigma.kappa.messages.ServerMessages.LaunchModel
import org.denigma.kappa.messages.{KappaFile, UIMessage}
import org.denigma.kappa.notebook.graph.layouts.ForceLayoutParams

/**
  * Created by antonkulaga on 8/14/16.
  */
object Commands {

  case object SaveAll extends UIMessage

  case class SetLayoutParameters(graph: String, parameters: ForceLayoutParams) extends UIMessage

  case class OpenFiles(files: List[KappaFile]) extends UIMessage

  case class CloseFile(path: String) extends UIMessage

  case class CloseSimulation(token: Int, initial: Option[LaunchModel]) extends UIMessage

  case class PauseSimulation(token: Int) extends UIMessage

  case class ContinueSimulation(token: Int) extends UIMessage


}

