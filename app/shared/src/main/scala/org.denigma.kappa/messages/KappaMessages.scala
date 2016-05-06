package org.denigma.kappa.messages

import org.denigma.controls.charts.{LineStyles, Point, Series}

sealed trait KappaMessage

object EmptyKappaMessage extends KappaMessage

import scala.collection.immutable._


case class KappaUser(name: String) extends KappaMessage

case class Connected(username: String, channel: String, users: List[KappaUser], servers: ConnectedServers = ConnectedServers.empty) extends KappaMessage

case class Disconnected(username: String, channel: String /*, time: LocalDateTime = LocalDateTime.now()*/) extends KappaMessage

trait ServerMessage extends KappaMessage

trait ErrorMessage extends KappaMessage
{
  def errors: List[String]
}

case class SyntaxErrors(server: String, errors: List[String], initialParams: Option[RunModel] = None) extends ServerMessage with ErrorMessage

case class SimulationResult(server: String, simulationStatus: SimulationStatus, token: Int, initialParams: Option[RunModel] = None) extends ServerMessage

case class LaunchModel(server: String, parameters: RunModel) extends ServerMessage

trait KappaFileMessage extends KappaMessage
