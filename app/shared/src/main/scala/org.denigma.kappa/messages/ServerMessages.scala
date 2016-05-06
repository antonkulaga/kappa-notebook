package org.denigma.kappa.messages

import scala.collection.immutable.{List, Nil}


object ServerConnection {
  lazy val default: ServerConnection = new ServerConnection("localhost", "localhost", 8080)
}

case class ServerConnection(name: String, host: String, port: Int) extends ServerMessage {
  def server = name
}

object ConnectedServers {
  lazy val empty: ConnectedServers = ConnectedServers(Nil)

}

case class ConnectedServers(servers: List[ServerConnection]) extends ServerMessage

case class ServerErrors(errors: List[String]) extends ErrorMessage with ServerMessage

case class Connect(server: ServerConnection) extends ServerMessage

