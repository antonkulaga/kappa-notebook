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
                         serverConnections: Var[ServerConnections],
                         configurationName: Var[String] = Var("")
                        ) extends Circuit(input, output){


  val simulationResults = Var(Map.empty[ (Int, Option[LaunchModel]), SimulationStatus])

  val configurations: Var[Map[String, SourcesFileSelector]] = Var(Map(("", DefaultSourceSelector)))

  val currentConfiguration: Rx[SourcesFileSelector] = Rx{
    val name = configurationName()
    val mp = configurations()
    mp.getOrElse(name, DefaultSourceSelector)
  }

  val runnableFiles: Rx[List[KappaSourceFile]] = Rx{
    val proj = currentProject()
    val cur = currentConfiguration()
    cur(proj)
  }

  val serverErrors = Var(ServerErrors.empty)

  val kappaServerErrors = Var(KappaServerErrors.empty)


  protected def onInputMessage(message: KappaMessage): Unit = message match {

    case KappaMessage.ServerResponse(server, ers: ServerErrors) =>  serverErrors() = ers

    case KappaMessage.ServerResponse(server, ers: KappaServerErrors) => kappaServerErrors() = ers

    case Failed(operation, ers, username) =>  kappaServerErrors() = kappaServerErrors.now.copy(errors = kappaServerErrors.now.errors ++ ers)

    case KappaMessage.ServerResponse(server, SimulationResult(status, token, params) ) =>
      kappaServerErrors() = KappaServerErrors.empty
      simulationResults() = simulationResults.now.updated((token, params), status)
    //require(items.now.exists{ case (key, value) => value==status}, "status should be added to items")

    case s: ServerErrors =>
      dom.console.error("server errors = "+s.errors)
      serverErrors() = s

    case KappaMessage.ServerResponse(server, s: SyntaxErrors) =>

    //errors() = s.errors.map(e=>e.fullMessage)

    case Commands.CloseSimulation(token) =>
      simulationResults.now.keys.collectFirst{
        case (t, l)  if t == token => (t, l)
      } match {
        case Some(key) =>simulationResults() = simulationResults.now - key
        case None => dom.console.error(s"simulation with token ${token} cannot be closed because it was not found")
      }

    case other => //do nothing
  }

  val launcher = Var((LaunchModel.empty, ""))
  launcher.onChange{
    case (value, name) if configurations.now.contains(name) =>
      val files = runnableFiles.now.map(f=>(f.path, f.content))
      val message = ServerCommand(serverConnections.now.currentServer, value.copy(files = files))
      input() = message

    case (value, name) =>
      dom.console.error(s"cannot find configuration $name, run with the default one")
      val files = runnableFiles.now.map(f=>(f.path, f.content))
      val message = ServerCommand(serverConnections.now.currentServer, value.copy(files = files))
      input() = message
  }

}
