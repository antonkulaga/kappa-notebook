package org.denigma.kappa.messages

import boopickle.CompositePickler
import org.denigma.kappa.messages.ServerMessages.{ConnectedServers, ServerMessage}
import boopickle.DefaultBasic._
object KappaMessage{

  object ServerCommand {
    implicit val classPickler: Pickler[ServerCommand] = boopickle.Default.generatePickler[ServerCommand]

    lazy val defaultServer = ""

    def onDefaultServer(message: ServerMessage) = ServerCommand(defaultServer, message)
  }

  case class ServerCommand(server: String, serverMessage: ServerMessage) extends KappaMessage

  object ServerResponse {
    implicit val classPickler: Pickler[ServerResponse] = boopickle.Default.generatePickler[ServerResponse]
  }

  case class ServerResponse(server: String, serverMessage: ServerMessage) extends KappaMessage


  object Container {

    implicit val classPickler = boopickle.Default.generatePickler[Container]

    def apply(messages: KappaMessage*): Container = Container(messages.toList)
  }
  case class Container(messages: List[KappaMessage] , betweenInterval: Int = 0) extends KappaMessage
  {
    def andThen(message: KappaMessage): Container = copy(messages :+ message)
  }

  implicit val classPickler: CompositePickler[KappaMessage] = compositePickler[KappaMessage]
    .addConcreteType[ServerErrors]
    .addConcreteType[Connected]
    .addConcreteType[Disconnected]
    .addConcreteType[Done]
    .addConcreteType[Failed]
    .addConcreteType[IncomingFailed]
    .addConcreteType[Container]
    .addConcreteType[KappaUser]
    .addConcreteType[ServerCommand]
    .addConcreteType[ServerResponse]
    .addConcreteType[FileResponses.SavedFiles]
    .addConcreteType[KeepAlive]
    .join(KappaFileMessage.kappaFilePickler)
    .join(UIMessage.UIMessagePickler)

}

trait KappaMessage
object KeepAlive {
  implicit val classPickler: Pickler[KeepAlive] = boopickle.Default.generatePickler[KeepAlive]
}
case class KeepAlive(username: String) extends KappaMessage //to overcome buggy akka timeouts

object ServerErrors {
  implicit val classPickler: Pickler[ServerErrors] = boopickle.Default.generatePickler[ServerErrors]
  lazy val empty = ServerErrors(Nil)
}

case class ServerErrors(errors: List[String]) extends KappaMessage

case object EmptyKappaMessage extends KappaMessage

object KappaUser{
  implicit val classPickler: Pickler[KappaUser] = boopickle.Default.generatePickler[KappaUser]
}

case class KappaUser(name: String) extends KappaMessage

object Connected{
  implicit val classPickler: Pickler[Connected] = boopickle.Default.generatePickler[Connected]
}

case class Connected(username: String, channel: String, users: List[KappaUser], servers: ConnectedServers = ConnectedServers.empty) extends KappaMessage

object Disconnected{
  implicit val classPickler: Pickler[Disconnected] = boopickle.Default.generatePickler[Disconnected]
}

case class Disconnected(username: String, channel: String, users: List[KappaUser] /*, time: LocalDateTime = LocalDateTime.now()*/) extends KappaMessage


object Done{
  implicit val classPickler: Pickler[Done] = boopickle.Default.generatePickler[Done]
}

case class Done(operation: KappaMessage, user: String) extends KappaMessage

object Failed{
  implicit val classPickler: Pickler[Failed] = boopickle.Default.generatePickler[Failed]
}

case class Failed(operation: KappaMessage, errors: List[String], user: String) extends KappaMessage

object IncomingFailed{
  implicit val classPickler: Pickler[IncomingFailed] = boopickle.Default.generatePickler[IncomingFailed]
}

case class IncomingFailed(reason: String, user: String) extends KappaMessage

