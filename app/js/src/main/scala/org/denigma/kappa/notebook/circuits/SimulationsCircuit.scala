package org.denigma.kappa.notebook.circuits

import org.denigma.binding.extensions._
import org.denigma.kappa.messages.KappaMessage.ServerCommand
import org.denigma.kappa.messages.ServerMessages._
import org.denigma.kappa.messages.WebSimMessages.SimulationStatus
import org.denigma.kappa.messages.{DefaultSourceSelector, _}
import org.denigma.kappa.notebook.actions.Commands
import org.denigma.kappa.notebook.views.common.ServerConnections
import org.scalajs.dom
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

class SimulationsCircuit(input: Var[KappaMessage],
                         output: Var[KappaMessage],
                         currentProject: Rx[KappaProject],
                         val serverConnections: Var[ServerConnections],
                         val configurationName: Var[String] = Var("")
                        ) extends Circuit(input, output) {


  val projectName = currentProject.map(p=>p.name)

  val simulationResults = Var(Map.empty[(Int, Option[LaunchModel]), SimulationStatus])

  val configurations: Var[Map[String, SourcesFileSelector]] = Var(Map(("", DefaultSourceSelector)))

  val sourceFileSelector: Rx[SourcesFileSelector] = Rx {
    val name = configurationName()
    val mp = configurations()
    mp.getOrElse(name, DefaultSourceSelector)
  }

  val launcher = Var((LaunchModel.empty, ""))
  launcher.onChange{
    case (value, name) if configurations.now.contains(name) =>
      val files = runConfiguration.now.files.map(f=>(f.path, f.content))
      val message = ServerCommand(serverConnections.now.currentServer, value.copy(files = files))
      output() = message

    case (value, name) =>
      dom.console.error(s"cannot find configuration $name, run with the default one")
      val files = runConfiguration.now.files.map(f=>(f.path, f.content))
      val message = ServerCommand(serverConnections.now.currentServer, value.copy(files = files))
      output() = message
  }

  val runConfiguration: Rx[RunConfiguration] = Rx {
    val proj = currentProject()
    val cur = sourceFileSelector()
    val files: List[KappaSourceFile] = cur(proj)
    val l = launcher()
    val (params, name) = l
    RunConfiguration(files, params, projectName(), name, serverConnections.now.currentConnection)
  }


  protected def onInputMessage(message: KappaMessage): Unit = message match {

    case KappaMessage.ServerResponse(server, SimulationResult(status, token, params) ) =>
      simulationResults() = simulationResults.now.updated((token, params), status)
    //require(items.now.exists{ case (key, value) => value==status}, "status should be added to items")

    case Commands.CloseSimulation(token) =>
      simulationResults.now.keys.collectFirst{
        case (t, l)  if t == token => (t, l)
      } match {
        case Some(key) =>simulationResults() = simulationResults.now - key
        case None => dom.console.error(s"simulation with token ${token} cannot be closed because it was not found")
      }

    case other => //do nothing
  }

}
