package org.denigma.kappa.notebook.communication

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import java.time.LocalDateTime

/**
  * Created by antonkulaga on 10/03/16.
  */
object SocketMessages {
  trait SocketMessage

  trait ChannelMessage extends SocketMessage
  {
    def channel: String
    def username: String
  }

  trait WebSocketMessage extends ChannelMessage{
    def message: Message
   }

  case class IncomingMessage(channel: String, username: String, message: Message, time: LocalDateTime = LocalDateTime.now()) extends WebSocketMessage
  case class OutgoingMessage(channel: String, username: String, message: Message, time: LocalDateTime = LocalDateTime.now()) extends WebSocketMessage

  case class UserJoined(username: String, channel: String, actorRef: ActorRef, time: LocalDateTime = LocalDateTime.now()) extends ChannelMessage
  case class UserLeft(username: String, channel: String, time: LocalDateTime = LocalDateTime.now()) extends  ChannelMessage
}