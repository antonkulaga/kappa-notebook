package org.denigma.kappa.notebook.communication

import akka.actor.{Actor, ActorLogging, ActorRef}
import org.denigma.kappa.notebook.services.WebSimClient

class RoomActor(channel: String) extends Actor with ActorLogging{

  var participants: Map[String, ActorRef] = Map.empty[String, ActorRef]

  override def receive: Receive = {
    case SocketMessages.UserJoined(name, channel, actorRef, time) =>
      participants += name -> actorRef
      //broadcast(SystemMessage(s"User $name joined channel..."))
      println(s"User $name joined channel[$channel]")

    case SocketMessages.UserLeft(name, channel, time) =>
      println(s"User $name left channel[$channel]")
      //broadcast(SystemMessage(s"User $name left channel[$roomId]"))
      participants -= name

    case msg @ SocketMessages.IncomingMessage(channel, username, message, time) =>
      /*
      participants.get(username) match
      {
        case Some(user) => user ! msg
        case None => log.error(s"message for nonexistent participant $username")

      }
      */
      broadcast(msg) //TODO: fix
  }

  def broadcast(message: SocketMessages.IncomingMessage): Unit = participants.values.foreach(_ ! message)

}
