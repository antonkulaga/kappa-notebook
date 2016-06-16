package org.denigma.kappa.messages

import scala.collection.immutable.{List, Nil}
import boopickle.DefaultBasic._

object ServerMessage {
  /*
  implicit val classPickler = compositePickler[ServerMessage]
    .addConcreteType[ConnectedServers]
    .addConcreteType[ServerConnection]
    .addConcreteType[ServerErrors]
    .addConcreteType[SyntaxErrors]
    .addConcreteType[Connect]
    .addConcreteType[SimulationResult]
    .addConcreteType[LaunchModel]
  */
}

trait ServerMessage



object ServerConnection {
  implicit val classPickler = boopickle.Default.generatePickler[ServerConnection]

  lazy val default: ServerConnection = new ServerConnection("localhost", "localhost", 8080)
}

case class ServerConnection(name: String, host: String, port: Int) extends ServerMessage {
  def server = name
}

object ConnectedServers {
  lazy val empty: ConnectedServers = ConnectedServers(Nil)
  implicit val classPickler = boopickle.Default.generatePickler[ConnectedServers]
}

case class ConnectedServers(servers: List[ServerConnection]) extends ServerMessage

object ServerErrors {
  implicit val classPickler = boopickle.Default.generatePickler[ServerErrors]
}
case class ServerErrors(errors: List[String]) extends ErrorMessage with ServerMessage

object Connect
{
  implicit val classPickler = boopickle.Default.generatePickler[Connect]
}
case class Connect(server: ServerConnection) extends ServerMessage

trait ErrorMessage
{
  def errors: List[String]
}

object SyntaxErrors {

  implicit val classPickler = boopickle.Default.generatePickler[SyntaxErrors]
}
case class SyntaxErrors(server: String, errors: List[String], initialParams: Option[RunModel] = None) extends ServerMessage with ErrorMessage

object SimulationResult {
  implicit val classPickler = boopickle.Default.generatePickler[SimulationResult]
}
case class SimulationResult(server: String, simulationStatus: SimulationStatus, token: Int, initialParams: Option[RunModel] = None) extends ServerMessage

object LaunchModel {
  implicit val classPickler = boopickle.Default.generatePickler[LaunchModel]
}
case class LaunchModel(server: String, parameters: RunModel, counter: Int = 0) extends ServerMessage
