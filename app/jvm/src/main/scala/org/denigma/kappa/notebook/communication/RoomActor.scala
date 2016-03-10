package org.denigma.kappa.notebook.communication

import akka.actor.{Actor, ActorRef}
import org.denigma.kappa.notebook.services.WebSimClient

class RoomActor(channel: String) extends Actor {

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

    case msg: SocketMessages.IncomingMessage =>  broadcast(msg) //TODO: fix
  }

  def broadcast(message: SocketMessages.IncomingMessage): Unit = participants.values.foreach(_ ! message)

}
