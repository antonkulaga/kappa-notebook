package org.denigma.kappa.notebook.communication

import java.io.{InputStream, File => JFile}

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.actor.ActorPublisherMessage
import akka.util.ByteString
import boopickle.DefaultBasic._
import org.denigma.kappa.messages.KappaMessage.{ServerCommand, ServerResponse}
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Actors that serves the user that is connected by a WebSocket connection
  * @param username name of the user
  * @param servers websim server actorref
  * @param fileManager class that is responsible for the file management
  */
class UserActor(val username: String, servers: ActorRef, val fileManager: FileManager)
  extends FileMessenger //contains partial function that takes care of File messages
    with ProjectMessenger //contains partial function that takes care of Project messages
{

  def readResource(path: String): Iterator[String] = {
    val stream: InputStream = getClass.getResourceAsStream(path)
    scala.io.Source.fromInputStream( stream ).getLines
  }


  /**
    * Main actor receive function
    * @return
    */
  override def receive: Receive =  onTextMessage
    .orElse(onBinaryMessage)
    .orElse(onKappaMessage)
    .orElse(onOtherMessage)

  /**
    * Websockets can send text and binary messages,
    * this partial function takes care of text ones
    * @return
    */
  protected def onTextMessage: Receive = {
    case SocketMessages.IncomingMessage(channel, uname, TextMessage.Strict(text), time) =>
     log.error("text message is :"+text)

    case SocketMessages.IncomingMessage(channel, uname, message: TextMessage.Streamed, time) =>

    message.textStream.runReduce[String]{
        case (a, b) => a ++ b
      }.onComplete{
        case Failure(th)=> log.error("Binary streaming failed")
          val d = Pickle.intoBytes[KappaMessage](IncomingFailed("cannot open text stream", username))
          send(d)

        case Success(text) =>
          log.error("text message is :"+text)

      }
  }


  /**
    * Function that takes care of all binary websocket messages,
    * unpickles and wraps them and send for further processing
    * @return
    */
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

  /**
    * this partial function takes care of messages about running websim simulations
    * @return
    */
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
      kappaHandler(message)
  }

  /**
    * Joined partial function that takes care of all KappaMessages
    */
  lazy val kappaHandler: PartialFunction[KappaMessage, Unit] =
    containerMessages
      .orElse(onServerMessage)
      .orElse(fileMessages)
      .orElse(projectMessages)
      .orElse(simulationMessages)
      .orElse(otherKappaMessages)

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

    case ActorPublisherMessage.Request(n) => deliverBuf() //it is used to integrate the actor into akka streams

    case other => log.error(s"Unknown other message: $other")
  }


  override def preStart() =
  {
   super.preStart()
   context.system.scheduler.schedule(10 seconds, 10 seconds){ //TODO: rewrite to timeout configs
    val d = Pickle.intoBytes[KappaMessage](KeepAlive(username))
      send(d)
    }



  }

}
