package org.denigma.kappa.notebook

import java.nio.ByteBuffer

import boopickle.Default._
import org.denigma.binding.extensions._
import org.denigma.controls.sockets.{BinaryWebSocket, WebSocketSubscriber}
import org.denigma.kappa.messages.{KappaPicklers, KappaMessages}
import org.denigma.kappa.messages.KappaMessages.{Container, Message}
import org.scalajs.dom
import rx.core.Var
import rx.ops._

import scala.collection.immutable._
object KappaHub{
  def empty: KappaHub = KappaHub(
    Var(KappaMessages.Code.empty),
    Var(KappaMessages.RunParameters()),
    Var(KappaMessages.Console.empty),
    Var(KappaMessages.Output.empty)
  )
}
case class KappaHub(
  code: Var[KappaMessages.Code],
  runParameters: Var[KappaMessages.RunParameters],
  console: Var[KappaMessages.Console],
  output: Var[KappaMessages.Output]
){
  def packContainer(): Container = KappaMessages.Container(List(code.now, runParameters.now))

  val chart = output.map(o => KappaMessages.Chart.parse(o))
}

case class WebSocketTransport(subscriber: WebSocketSubscriber, kappaHub: KappaHub) extends KappaPicklers with BinaryWebSocket
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
    case message: KappaMessages.Console => kappaHub.console() = message
    case message: KappaMessages.Output=> kappaHub.output() = message
    case message: KappaMessages.Code => kappaHub.code() = message
    case message: KappaMessages.Container => message.messages.foreach(receive)
    case other => dom.console.error(s"UNKNOWN KAPPA MESSAGE RECEIVED! "+other)

  }



}