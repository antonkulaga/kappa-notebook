package org.denigma.kappa.notebook.circuits

import org.denigma.binding.extensions._
import org.denigma.kappa.messages.KappaMessage.ServerCommand
import org.denigma.kappa.messages.ServerMessages._
import org.denigma.kappa.messages.WebSimMessages.{RunParameters, SimulationStatus}
import org.denigma.kappa.messages.{DefaultSourceSelector, _}
import org.denigma.kappa.notebook.actions.Commands
import org.denigma.kappa.notebook.views.common.ServerConnections
import org.scalajs.dom
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

/**
  * Circuit that handles simulations-related events
  * @param input input messages Rx
  * @param output is used to send messages to the server
  * @param currentProject current loaded project
  * @param serverConnections
  * @param configurationName
  */
class SimulationsCircuit(input: Var[KappaMessage],
                         output: Var[KappaMessage],
                         currentProject: Rx[KappaProject],
                         val serverConnections: Var[ServerConnections],
                         val configurationName: Var[String] = Var("")
                        ) extends Circuit(input, output) {

  val projectName = currentProject.map(p=>p.name)

  type Token = (Int, Option[LaunchModel])

  val openOrder: Var[List[Token]] = Var(Nil)

  val simulationResults = Var(Map.empty[Token, SimulationStatus])

  val items = Var(Map.empty[String, KappaSourceFile])

  simulationResults.updates.onChange(updateOpened)

  protected def updateOpened(upd: MapUpdate[Token, SimulationStatus]) = {
    openOrder() = openOrder.now.filterNot(o=>upd.removed.contains(o)) ++ upd.added.keysIterator.toList
  }

  val configurations: Var[Map[String, SourcesFileSelector]] = Var(Map(("", DefaultSourceSelector)))

  val sourceFileSelector: Rx[SourcesFileSelector] = Rx {
    val name = configurationName()
    val mp = configurations()
    mp.getOrElse(name, DefaultSourceSelector)
  }

  val run: Var[RunParameters] = Var(LaunchModel.empty)

  val files2Run: Rx[List[KappaSourceFile]] = Rx {
    val proj = currentProject()
    val cur = sourceFileSelector()
    cur(proj)
  }

  val tuples2Run: Rx[List[(String, String)]] = files2Run.map(files => files.map(f=>f.path->f.content))

  val runConfiguration: Rx[RunConfiguration] = Rx {
    RunConfiguration(files2Run(), run(), projectName.now, configurationName.now, serverConnections.now.currentConnection)
  }

  run.onChange{ params =>
    val toRun = runConfiguration.now
    //println("========CODE TO RUN====================\n"+toRun.fullCode)
    output() = ServerCommand(serverConnections.now.currentServer, toRun.launchModel)
  }

  tuples2Run.onChange { values=>  output() = ServerCommand(serverConnections.now.currentServer, ParseModel(values)) }


  protected def onInputMessage(message: KappaMessage): Unit = message match {

    case KappaMessage.ServerResponse(server, SimulationResult(status, token, params) ) =>
      simulationResults() = simulationResults.now.updated((token, params), status)
    //require(items.now.exists{ case (key, value) => value==status}, "status should be added to items")

    case Commands.CloseSimulation(token, opt) =>
      simulationResults.now.keys.collectFirst{
        case (t, l)  if t == token && l == opt=> (t, l)
      } match {
        case Some(key) =>
          println(s"closing simulations ${token}")
          simulationResults() = simulationResults.now - key
        case None => dom.console.error(s"simulation with token ${token} cannot be closed because it was not found")
      }

    case other => //do nothing
  }

}
