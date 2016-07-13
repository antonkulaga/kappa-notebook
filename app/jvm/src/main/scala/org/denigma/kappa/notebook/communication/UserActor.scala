package org.denigma.kappa.notebook.communication

import java.io.{InputStream, File => JFile}
import java.nio.ByteBuffer

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.actor.ActorPublisherMessage
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import boopickle.DefaultBasic._
import org.denigma.kappa.messages.KappaMessage.{ServerCommand, ServerResponse}
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class UserActor(val username: String, servers: ActorRef, val fileManager: FileManager) extends FileMessenger with ProjectMessenger
{

  def readResource(path: String): Iterator[String] = {
    val stream: InputStream = getClass.getResourceAsStream(path)
    scala.io.Source.fromInputStream( stream ).getLines
  }

  protected def onTextMessage: Receive = {
    case SocketMessages.IncomingMessage(channel, uname, TextMessage.Strict(text), time) =>
     log.error("text message is :"+text)

    case SocketMessages.IncomingMessage(channel, uname, message: TextMessage.Streamed, time) =>

    message.textStream.runReduce[String]{
        case (a, b) => a++b
      }.onComplete{
        case Failure(th)=> log.error("Binary streaming failed")
          val d = Pickle.intoBytes[KappaMessage](IncomingFailed("cannot open text stream", username))
          send(d)

        case Success(text) =>
          log.error("text message is :"+text)

      }
  }

  protected def simulationMessages: Receive  = {
    case ServerCommand(server, l: ServerMessages.LaunchModel)=>
      val toServer = RunAtServer(username, server, l, self, 100 millis)
      servers ! toServer

    case ServerCommand(server, p: ServerMessages.ParseModel) =>
      val toServer = RunAtServer(username, server, p, self, 100 millis)
      servers ! toServer
  }


  protected def otherKappaMessages: Receive  = {
    case other => log.error(s"unexpected $other")
  }

  def onKappaMessage: Receive = {
    case message: KappaMessage =>
      //println("received :")
      //pprint.pprintln(message)
      kappaHandler(message)
  }

  lazy val kappaHandler: PartialFunction[KappaMessage, Unit] =
    containerMessages
      .orElse(onServerMessage)
      .orElse(fileMessages)
      .orElse(projectMessages)
      .orElse(simulationMessages)
      .orElse(otherKappaMessages)



  protected def onBinaryMessage: Receive = {
    case SocketMessages.IncomingMessage(channel, uname, message: BinaryMessage.Strict, time) =>
      val mes: KappaMessage = Unpickle[KappaMessage].fromBytes(message.data.toByteBuffer)
      self ! mes

    case SocketMessages.IncomingMessage(channel, uname, message: BinaryMessage.Streamed, time) =>
      //message.dataStream.runWi
      val bytes: Future[ByteString] = message.dataStream.runReduce[ByteString]{
        case (a, b) => a ++ b
      }
     bytes.onComplete{
        case Failure(th)=>
          log.error("Binary streaming failed")
          val d = Pickle.intoBytes[KappaMessage](IncomingFailed("cannot open binary stream", username))
          send(d)

        case Success(data) =>
          log.info("Binary streaming received!")
          val mes: KappaMessage = Unpickle[KappaMessage].fromBytes(data.toByteBuffer)
          self ! mes
      }


  }

  protected def onServerMessage: Receive = {

    case result: ServerResponse =>
      val d = Pickle.intoBytes[KappaMessage](result)
      //println("send server response:")
      //pprint.pprintln(result)
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


  override def receive: Receive =  onTextMessage
    .orElse(onBinaryMessage)
    .orElse(onKappaMessage)
    .orElse(onOtherMessage)

  context.system.scheduler.schedule(10 seconds, 20 seconds){ //TODO: rewrite to timeout configs
    println(s"keep $username alive")
    val d = Pickle.intoBytes[KappaMessage](KeepAlive(username))
    send(d)
  }

}
