package org.denigma.kappa.notebook.communication

import java.io.InputStream

import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.actor.{ActorPublisher, ActorPublisherMessage}
import akka.util.ByteString
import boopickle.Default._
import org.denigma.kappa.notebook.communication.SocketMessages.OutgoingMessage

import scala.annotation.tailrec
import scala.concurrent.duration._
import java.time._

import akka.http.scaladsl.model._
import org.denigma.kappa.messages._
import better.files._
import java.io.{File => JFile}

import org.denigma.kappa.notebook.FileManager


/**
  * This actor is creates for each user that connects via websocket
  * @param username
  * @param servers
  */
class UserActor(username: String, servers: ActorRef, fileManager: FileManager) extends KappaPicklers
  with Actor
  with akka.actor.ActorLogging
  with ActorPublisher[SocketMessages.OutgoingMessage]{

  implicit def ctx = context.dispatcher

  val MaxBufferSize = 100
  var buf = Vector.empty[OutgoingMessage]

  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      /*
      * totalDemand is a Long and could be larger than
      * what buf.splitAt can accept
      */
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }

  //val servers: Map[String, WebSimClient] = Map("default", new WebSimClient()(this.context.system, ActorMaterializer())) //note: will be totally rewritten


  def readResource(path: String): Iterator[String] = {
    val stream: InputStream = getClass.getResourceAsStream(path)
    scala.io.Source.fromInputStream( stream ).getLines
  }


  def run(params: RunModel): Unit = {
    val toServer = RunAtServer(username, "localhost", params, self, 100 millis)
    servers ! toServer
  }


  protected def onTextMessage: Receive = {
    case SocketMessages.IncomingMessage(channel, uname, TextMessage.Strict(text), time) =>
    /*
      val dec = parser.decode[WebSim.KappaMessage](text)
      dec match {
        case Xor.Right(value) => value match {
          //case status: WebSim.SimulationStatus =>
          case params: WebSim.RunModel =>
            run(params)
          case other => println(s"some other WebSim: \n $other")
        }
        case Xor.Left(failure) =>
          log.error(s"JSON failure detectedc $failure")
      }
      */
  }

  protected def onBinaryMessage: Receive = {
    case SocketMessages.IncomingMessage(channel, uname, message: BinaryMessage.Strict, time) =>
      Unpickle[KappaMessage].fromBytes(message.data.toByteBuffer) match
      {
        case Load(pro) =>

          //val rep = fileManager.cd("repressilator").read("repress.ka")
          //val code = Code(rep)//Code(readResource("/examples/abc.ka").mkString("\n"))
          val project = fileManager.loadProject(pro)
          val list = fileManager.loadProjectSet().map(p=> if(p.name==project.name) project else p).toList

          val d = Pickle.intoBytes[KappaMessage](Loaded(project, list))
          send(BinaryMessage(ByteString(d)))

        case LaunchModel(server, parameters)=> run(parameters)

        case other => log.error(s"unexpected $other")
      }
    //log.error(s"something binary received on $channel by $username")
  }

  protected def onServerMessage: Receive = {

    case result: SimulationResult =>
      println("on server")
      val d = Pickle.intoBytes[KappaMessage](result)
      send(BinaryMessage(ByteString(d)))

    case s: SyntaxErrors=>
      val d = Pickle.intoBytes[KappaMessage](s)
      send(BinaryMessage(ByteString(d)))

    case result: Connected =>
      val d = Pickle.intoBytes[KappaMessage](result)
      send(BinaryMessage(ByteString(d)))

    case Disconnected(user, channel) =>
      log.info(s"User $user disconnected from channel $channel")



  }

  protected def onOtherMessage: Receive = {

    case ActorPublisherMessage.Request(n) => deliverBuf()

    case other => log.error(s"Unknown other message: $other")
  }


  override def receive: Receive =  onTextMessage.orElse(onBinaryMessage).orElse(onServerMessage).orElse(onOtherMessage)

  def deliver(mess: OutgoingMessage) = {
    if (buf.isEmpty && totalDemand > 0)
      onNext(mess)
    else {
      buf :+= mess
      deliverBuf()
    }
  }

  def send(textMessage: TextMessage, channel: String): Unit = {
    val message = SocketMessages.OutgoingMessage(channel, username, textMessage, LocalDateTime.now)
    deliver(message)
  }

  def send(binaryMessage: BinaryMessage, channel: String = "all") = {
    val message = SocketMessages.OutgoingMessage(channel, username, binaryMessage, LocalDateTime.now)
    deliver(message)
  }


}
