package org.denigma.kappa.notebook.communication

import akka.actor.Actor

import java.io.{InputStream, File => JFile}
import java.nio.ByteBuffer
import java.time._
import java.util.UUID

import akka.Done
import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.actor.{ActorPublisher, ActorPublisherMessage}
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import boopickle.DefaultBasic._
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager
import org.denigma.kappa.notebook.communication.SocketMessages.OutgoingMessage

import scala.annotation.tailrec
import scala.collection.immutable.SortedSet
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}


trait Messenger extends KappaPicklers
  with Actor
  with akka.actor.ActorLogging
  with ActorPublisher[SocketMessages.OutgoingMessage]
{

  implicit def ctx = context.dispatcher

  implicit val materializer = ActorMaterializer()

  def username: String


  val MaxBufferSize = 10000
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

  def sendBinary(binaryMessage: BinaryMessage, channel: String = "all") = {
    val message = SocketMessages.OutgoingMessage(channel, username, binaryMessage, LocalDateTime.now)
    deliver(message)
  }

  def send(d: ByteBuffer, channel: String = "all"): Unit = {
    sendBinary(BinaryMessage(ByteString(d)), channel)
  }

}
