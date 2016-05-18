package org.denigma.kappa.messages

import boopickle.CompositePickler

object KappaMessage{
  //import boopickle.DefaultBasic._

  import boopickle.Default._
  implicit val simpleMessagePickler: CompositePickler[KappaMessage] = compositePickler[KappaMessage]
    .addConcreteType[KappaProject]
    .addConcreteType[LaunchModel]
    .addConcreteType[SimulationResult]
    .addConcreteType[SyntaxErrors]
    .addConcreteType[ServerErrors]
    .addConcreteType[Connected]
    .addConcreteType[Disconnected]
    .addConcreteType[KappaFile]
    .addConcreteType[KappaFolder]
    .addConcreteType[ProjectRequests.Create]
    .addConcreteType[ProjectRequests.Download]

    .addConcreteType[ProjectRequests.Load]
    .addConcreteType[ProjectRequests.Save]
    .addConcreteType[ProjectResponses.Loaded]
    .addConcreteType[ProjectRequests.Remove]

    .addConcreteType[FileRequests.LoadFile]
    .addConcreteType[FileRequests.LoadFileSync]
    .addConcreteType[FileRequests.Remove]
    .addConcreteType[FileRequests.Upload]
    .addConcreteType[FileRequests.ZipUpload]

    .addConcreteType[FileResponses.Downloaded]
    .addConcreteType[FileResponses.UploadStatus]
    .addConcreteType[DataChunk]
    .addConcreteType[DataMessage]
    .addConcreteType[Done]
    .addConcreteType[Failed]

}

sealed trait KappaMessage

case object EmptyKappaMessage extends KappaMessage

import scala.collection.immutable._


case class KappaUser(name: String) extends KappaMessage

case class Connected(username: String, channel: String, users: List[KappaUser], servers: ConnectedServers = ConnectedServers.empty) extends KappaMessage

case class Disconnected(username: String, channel: String, users: List[KappaUser] /*, time: LocalDateTime = LocalDateTime.now()*/) extends KappaMessage

trait ServerMessage extends KappaMessage

trait ErrorMessage extends KappaMessage
{
  def errors: List[String]
}

case class SyntaxErrors(server: String, errors: List[String], initialParams: Option[RunModel] = None) extends ServerMessage with ErrorMessage

case class SimulationResult(server: String, simulationStatus: SimulationStatus, token: Int, initialParams: Option[RunModel] = None) extends ServerMessage

case class LaunchModel(server: String, parameters: RunModel, counter: Int = 0) extends ServerMessage

trait KappaFileMessage extends KappaMessage

case class DataChunk(id: String, path: String, data: Array[Byte], downloaded: Int, total: Int, completed: Boolean = false) extends KappaFileMessage
{
  lazy val percent = downloaded / total
}

case class DataMessage(name: String, data: Array[Byte]) extends KappaFileMessage

case class Done(operation: KappaMessage, user: String) extends KappaMessage

case class Failed(operation: KappaMessage, error: List[String], user: String) extends KappaMessage

