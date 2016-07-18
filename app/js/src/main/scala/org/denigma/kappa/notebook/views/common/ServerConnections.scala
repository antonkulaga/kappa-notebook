package org.denigma.kappa.notebook.views.common

import org.denigma.kappa.messages.ServerMessages.ServerConnection


object ServerConnections {
  lazy val default = ServerConnections(ServerConnection.default.name, Map(ServerConnection.default.name->ServerConnection.default))
}

case class ServerConnections(currentServer: String, all: Map[String, ServerConnection])
{
  lazy val currentConnection: Option[ServerConnection] = all.get(currentServer)

  lazy val isConnected: Boolean = currentConnection.isDefined
}
