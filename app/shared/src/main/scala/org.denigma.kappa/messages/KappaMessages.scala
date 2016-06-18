package org.denigma.kappa.messages

import boopickle.CompositePickler

object KappaMessage{

  object ServerCommand {
    import boopickle.DefaultBasic._
    implicit val classPickler = boopickle.Default.generatePickler[ServerCommand]
  }

  case class ServerCommand(serverMessage: ServerMessage) extends KappaMessage

  object ServerResponse {
    import boopickle.DefaultBasic._
    implicit val classPickler = boopickle.Default.generatePickler[ServerCommand]
  }

  case class ServerResponse(serverMessage: ServerMessage) extends KappaMessage


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
    .addConcreteType[ServerErrors]
    .addConcreteType[KappaProject]
    .addConcreteType[KappaFile]
    .addConcreteType[KappaFolder]
    .addConcreteType[Done]
    .addConcreteType[Failed]
    .addConcreteType[Container]

    .join(KappaFileMessage.kappaFilePickler)
    .join(UIMessage.UIMessagePickler)

}

sealed trait KappaMessage

object ServerErrors {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[ServerErrors] = boopickle.Default.generatePickler[ServerErrors]
  lazy val empty = ServerErrors(Nil)
}

case class ServerErrors(errors: List[String]) extends KappaMessage

object KappaFileMessage {
  import boopickle.DefaultBasic._
  implicit val kappaFilePickler: CompositePickler[KappaFileMessage] = compositePickler[KappaFileMessage]
    .addConcreteType[ProjectRequests.Create]
    .addConcreteType[ProjectRequests.Download]
    .addConcreteType[ProjectRequests.Load]
    .addConcreteType[ProjectRequests.Save]
    .addConcreteType[ProjectResponses.Loaded]
    .addConcreteType[ProjectRequests.Remove]

    .addConcreteType[FileRequests.LoadBinaryFile]
    .addConcreteType[FileRequests.LoadFileSync]
    .addConcreteType[FileRequests.Remove]
    .addConcreteType[FileRequests.Save]
    .addConcreteType[FileRequests.ZipUpload]

    .addConcreteType[FileResponses.Downloaded]
    .addConcreteType[FileResponses.UploadStatus]

    .addConcreteType[DataChunk]
    .addConcreteType[DataMessage]
    .join(KappaPath.kappaPathPickler)

}

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

object KappaUser{
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[KappaUser] = boopickle.Default.generatePickler[KappaUser]
}

case class KappaUser(name: String) extends KappaMessage

object Connected{
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[Connected] = boopickle.Default.generatePickler[Connected]
}

case class Connected(username: String, channel: String, users: List[KappaUser], servers: ConnectedServers = ConnectedServers.empty) extends KappaMessage

object Disconnected{
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[Disconnected] = boopickle.Default.generatePickler[Disconnected]
}

case class Disconnected(username: String, channel: String, users: List[KappaUser] /*, time: LocalDateTime = LocalDateTime.now()*/) extends KappaMessage

object DataChunk{
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[DataChunk] = boopickle.Default.generatePickler[DataChunk]
}

case class DataChunk(id: KappaMessage, path: String, data: Array[Byte], downloaded: Int, total: Int, completed: Boolean = false) extends KappaFileMessage
{
  lazy val percent = downloaded / total
}

object DataMessage{
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[DataMessage] = boopickle.Default.generatePickler[DataMessage]
}

case class DataMessage(name: String, data: Array[Byte]) extends KappaFileMessage

object Done{
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[Done] = boopickle.Default.generatePickler[Done]
}

case class Done(operation: KappaMessage, user: String) extends KappaMessage

object Failed{
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[Failed] = boopickle.Default.generatePickler[Failed]
}

case class Failed(operation: KappaMessage, errors: List[String], user: String) extends KappaMessage

