package org.denigma.kappa.messages

import boopickle.CompositePickler

object KappaMessage{

  object Container {
    import boopickle.DefaultBasic._

    implicit val classPickler = boopickle.Default.generatePickler[Container]

    def apply(messages: KappaMessage*): Container = Container(messages.toList)
  }
  case class Container(messages: List[KappaMessage]) extends KappaMessage
  {
    def andThen(message: KappaMessage): Container = copy(messages :+ message)
  }
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

    .addConcreteType[FileRequests.LoadBinaryFile]
    .addConcreteType[FileRequests.LoadFileSync]
    .addConcreteType[FileRequests.Remove]
    .addConcreteType[FileRequests.UploadBinary]
    .addConcreteType[FileRequests.ZipUpload]

    .addConcreteType[FileResponses.Downloaded]
    .addConcreteType[FileResponses.UploadStatus]
    .addConcreteType[DataChunk]
    .addConcreteType[DataMessage]
    .addConcreteType[Done]
    .addConcreteType[Failed]
    .addConcreteType[Container]
    .join(UIMessage.UIMessagePickler)

}

sealed trait KappaMessage
/*
object KappaFileMessage {
  import boopickle.DefaultBasic._
  implicit val simpleMessagePickler: CompositePickler[KappaFileMessage] = compositePickler[KappaFileMessage]
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
}
*/
trait KappaFileMessage extends KappaMessage


case object EmptyKappaMessage extends KappaMessage

import scala.collection.immutable._

object UIMessage {
  import boopickle.DefaultBasic._

  implicit val UIMessagePickler: CompositePickler[UIMessage] = compositePickler[UIMessage]
    //.addConcreteType[GoToPaper]
    .addConcreteType[Go.ToSource]
    .addConcreteType[Go.ToTab]
    .addConcreteType[MoveTo.Tab]
}


trait UIMessage extends KappaMessage

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


case class DataChunk(id: KappaMessage, path: String, data: Array[Byte], downloaded: Int, total: Int, completed: Boolean = false) extends KappaFileMessage
{
  lazy val percent = downloaded / total
}

case class DataMessage(name: String, data: Array[Byte]) extends KappaFileMessage

case class Done(operation: KappaMessage, user: String) extends KappaMessage

case class Failed(operation: KappaMessage, error: List[String], user: String) extends KappaMessage

