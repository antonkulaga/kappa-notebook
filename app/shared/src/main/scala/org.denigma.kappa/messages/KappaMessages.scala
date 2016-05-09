package org.denigma.kappa.messages

import boopickle.CompositePickler

object KappaMessage{
  //import boopickle.DefaultBasic._

  import boopickle.Default._
  implicit val simpleMessagePickler: CompositePickler[KappaMessage] = compositePickler[KappaMessage]
    .addConcreteType[Load]
    .addConcreteType[KappaProject]
    .addConcreteType[LaunchModel]
    .addConcreteType[SimulationResult]
    .addConcreteType[SyntaxErrors]
    .addConcreteType[ServerErrors]
    .addConcreteType[Connected]
    .addConcreteType[Disconnected]
    .addConcreteType[KappaFile]
    .addConcreteType[KappaFolder]
    .addConcreteType[Loaded]
    .addConcreteType[Remove]
    .addConcreteType[Create]
    .addConcreteType[Save]
    .addConcreteType[Done]
    .addConcreteType[Failed]
    .addConcreteType[Upload]
    .addConcreteType[ZipUpload]


}

sealed trait KappaMessage

case object EmptyKappaMessage extends KappaMessage

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

case class Done(operation: KappaMessage, user: String) extends KappaMessage

case class Failed(operation: KappaMessage, error: List[String], user: String) extends KappaMessage
