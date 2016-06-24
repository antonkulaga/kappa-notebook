package org.denigma.kappa.notebook.communication

import java.io.{File => JFile, InputStream}
import java.nio.ByteBuffer

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.actor.ActorPublisherMessage
import boopickle.DefaultBasic._
import org.denigma.kappa.messages.KappaMessage.{ServerCommand, ServerResponse}
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager

import scala.collection.immutable.SortedSet
import scala.concurrent.duration._

class UserActor(val username: String, servers: ActorRef, val fileManager: FileManager) extends FileMessenger with ProjectMessenger
{

  def readResource(path: String): Iterator[String] = {
    val stream: InputStream = getClass.getResourceAsStream(path)
    scala.io.Source.fromInputStream( stream ).getLines
  }

  protected def onTextMessage: Receive = {
    case SocketMessages.IncomingMessage(channel, uname, TextMessage.Strict(text), time) =>
  }

  protected def simulationMessages: Receive  = {
    case ServerCommand(l: ServerMessages.LaunchModel)=>
      val toServer = RunAtServer(username, l.server, l, self, 100 millis)
      //println("RUN QT SERVER: "+toServer)
      servers ! toServer

    case ServerCommand(p: ServerMessages.ParseModel) =>
      val toServer = RunAtServer(username, p.server, p, self, 100 millis)
      servers ! toServer
  }


  protected def otherKappaMessages: Receive  = {
    case other => log.error(s"unexpected $other")
  }


  protected def onBinaryMessage: Receive = {
    case SocketMessages.IncomingMessage(channel, uname, message: BinaryMessage.Strict, time) =>
      val mes = Unpickle[KappaMessage].fromBytes(message.data.toByteBuffer)
      val fun = fileMessages.orElse(projectMessages).orElse(simulationMessages).orElse(otherKappaMessages)
      fun(mes)
    //log.error(s"something binary received on $channel by $username")
  }

  protected def onServerMessage: Receive = {

    case result: ServerResponse =>
      val d = Pickle.intoBytes[KappaMessage](result)
      send(d)

    case result: Connected =>
      val d = Pickle.intoBytes[KappaMessage](result)
      send(d)

    case Disconnected(user, channel, list) =>
      log.info(s"User $user disconnected from channel $channel")

  }

  protected def onOtherMessage: Receive = {

    case ActorPublisherMessage.Request(n) => deliverBuf()

    case other => log.error(s"Unknown other message: $other")
  }


  override def receive: Receive =  onTextMessage.orElse(onBinaryMessage).orElse(onServerMessage).orElse(onOtherMessage)


}
