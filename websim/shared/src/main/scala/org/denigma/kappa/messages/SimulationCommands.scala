package org.denigma.kappa.messages

import boopickle.CompositePickler
import boopickle.DefaultBasic._
import org.denigma.kappa.messages.ServerMessages.{LaunchModel, ServerMessage}


object SimulationCommands {

  object SimulationCommand {
      implicit val classPickler: CompositePickler[SimulationCommand] = compositePickler[SimulationCommand]
        .addConcreteType[SimulationCommands.CloseSimulation]
        .addConcreteType[SimulationCommands.StopSimulation]
        .addConcreteType[SimulationCommands.PauseSimulation]
        .addConcreteType[SimulationCommands.ContinueSimulation]
  }

  trait SimulationCommand extends ServerMessage

  object CloseSimulation {
    implicit val classPickler: Pickler[CloseSimulation] = boopickle.Default.generatePickler[CloseSimulation]

  }

  case class CloseSimulation(token: Int, initial: Option[LaunchModel]) extends SimulationCommand

  object StopSimulation {
    implicit val classPickler: Pickler[StopSimulation] = boopickle.Default.generatePickler[StopSimulation]
  }

  case class StopSimulation(token: Int) extends SimulationCommand

  object PauseSimulation {
    implicit val classPickler: Pickler[PauseSimulation] = boopickle.Default.generatePickler[PauseSimulation]
  }

  case class PauseSimulation(token: Int) extends SimulationCommand

  object ContinueSimulation {
    implicit val classPickler: Pickler[ContinueSimulation] = boopickle.Default.generatePickler[ContinueSimulation]
  }

  case class ContinueSimulation(token: Int) extends SimulationCommand

}