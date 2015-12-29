package org.denigma.kappa.notebook

import java.nio.ByteBuffer

import boopickle.Default._
import org.denigma.binding.extensions._
import org.denigma.controls.sockets.{BinaryWebSocket, WebSocketSubscriber}
import org.denigma.kappa.messages.{KappaPicklers, KappaMessages}
import org.denigma.kappa.messages.KappaMessages.Message
import org.scalajs.dom
import rx.core.Var

import scala.collection.immutable._
object KappaHub{
  def empty = KappaHub(Var(None), Var(None), Var(None))
}
case class KappaHub(
  console: Var[Option[KappaMessages.Console]],
  chart: Var[Option[KappaMessages.Chart]],
  code: Var[Option[KappaMessages.Code]]
)

case class WebSocketConnector(subscriber: WebSocketSubscriber, kappaHub: KappaHub) extends KappaPicklers with BinaryWebSocket
{
  subscriber.onOpen.handler{
    dom.console.log("WebSocket has been opened")
    //send(disc)
  }

  def send(message: KappaMessages.Message): Unit = {
    val mes = bytes2message(Pickle.intoBytes(message))
    subscriber.send(mes)
  }

  subscriber.onClose.handler(
    dom.console.log("WebSocked has been closed")
  )
  subscriber.onMessage.onChange("OnMessage",uniqueValue = false)(onMessage)
  //chosen.onChange("chosenChange")(onChosenChange)


  override protected def updateFromMessage(bytes: ByteBuffer): Unit = receive(Unpickle[KappaMessages.Message].fromBytes(bytes))

  def receive: PartialFunction[KappaMessages.Message, Unit] = {
    case message: KappaMessages.Console => kappaHub.console() = Some(message)
    case message: KappaMessages.Chart => kappaHub.chart() = Some(message)
    case message: KappaMessages.Code => kappaHub.code() = Some(message)
    case message: KappaMessages.Container => message.messages.foreach(receive)
    case other => dom.console.log(s"UNKNOWN KAPPA MESSAGE RECEIVED! "+other)

  }



}