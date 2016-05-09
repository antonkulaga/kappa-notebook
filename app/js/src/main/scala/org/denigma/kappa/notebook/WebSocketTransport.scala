package org.denigma.kappa.notebook

import java.nio.ByteBuffer

import boopickle.DefaultBasic._
import org.denigma.binding.extensions._
import org.denigma.controls.papers.Bookmark
import org.denigma.controls.sockets.{BinaryWebSocket, WebSocketStorage, WebSocketSubscriber}
import org.denigma.kappa.messages._
//import org.denigma.kappa.notebook.storage.WebSocketStorage
import org.scalajs.dom
import org.scalajs.dom.raw.WebSocket
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Var

import scala.collection.immutable._
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js.timers

case class WebSocketTransport(channel: String, username: String) extends KappaPicklers with BinaryWebSocket with WebSocketSubscriber
{

  override def getWebSocketUri(username: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/channel/$channel?username=$username"
  }

  def open() = {
    urlOpt() = Option(getWebSocketUri(username))
  }

  onMessage.triggerLater{
    val mess = onMessage.now
    onMessage(mess)
  }


  override protected def updateFromMessage(bytes: ByteBuffer): Unit = {
    //println("from bytes fires")
    val message: KappaMessage = Unpickle[KappaMessage].fromBytes(bytes)
    //expectations() = updatedExpectations(message)
    onKappaMessage() = message
  }

  val onKappaMessage: Var[KappaMessage] = Var(EmptyKappaMessage)
  val sendKappaMessage: Var[KappaMessage] = Var(EmptyKappaMessage)
  sendKappaMessage.triggerLater{
    send(sendKappaMessage.now)
  }

  def send(message: KappaMessage): Unit = {
    val mes = bytes2message(Pickle.intoBytes(message))
    send(mes)
  }

  onClose.triggerLater {
    val message = "Websocket server connection has been closed"
    dom.console.error(message)
    //errors() = message::errors.now
  }


  /*
  lazy val onOpen: rx.Var[Event] = Var(Events.createEvent())
  lazy val onMessage: rx.Var[dom.MessageEvent] = Var(Events.createMessageEvent())
  lazy val onError: rx.Var[dom.ErrorEvent] = Var(Events.createErrorEvent())
  lazy val onClose: rx.Var[Event] = Var(Events.createEvent())
  */
  override def initWebSocket(url: String): WebSocket = WebSocketStorage(url)
}
/*
case class WebSocketTransport(
                               subscriber: WebSocketSubscriber,
                               errors: Var[List[String]])
                             (onopen: ()=>Unit)
                             (onmessage: PartialFunction[KappaMessage, Unit]) extends KappaPicklers with BinaryWebSocket
{
  type MessageHandler = PartialFunction[KappaMessage, Unit]

  subscriber.onOpen.triggerLater{
    dom.console.log("WebSocket has been opened")
    send(Load(KappaProject.default))
    //send(disc)
  }

  def send(message: KappaMessage): Unit = {
    val mes = bytes2message(Pickle.intoBytes(message))
    subscriber.send(mes)
  }


  def ask(message: KappaMessage, timeout: FiniteDuration)(handler: MessageHandler) = {
    expectations() = handler::expectations.now
    //println("from bytes fires")
    timers.setTimeout(timeout) {
      if(expectations.now.contains(handler))
      {
        val errorMessage = s"have not received an answer for the $message for $timeout duraction, please retry!"
        dom.console.error(errorMessage)
        errors() = List(errorMessage)
      }
    }
    send(message)
  }

  subscriber.onClose.triggerLater {
    val message = "Websocket server connection has been closed"
    dom.console.error(message)
    errors() = message::errors.now
  }

  /*
  TODO: fix this firefox bug
  this crashed firefox for some crazy reason!!!!!!!!!!!!!
  subscriber.onError.onChange(error=>
    dom.console.error("WebSocket had the following error: "+ error)
  )
*/


  subscriber.onMessage.onChange(onMessage)
  //chosen.onChange("chosenChange")(onChosenChange)

  override protected def updateFromMessage(bytes: ByteBuffer): Unit = {
    //println("from bytes fires")
    val message: KappaMessage = Unpickle[KappaMessage].fromBytes(bytes)
    expectations() = updatedExpectations(message)
    receive(message)
  }

  protected def updatedExpectations(message: KappaMessage) = expectations.now.foldLeft(List.empty[MessageHandler]) {
    case (acc, exp) =>
      if (exp.isDefinedAt(message)) {
        exp(message)
        acc
      } else exp :: acc
  }.reverse

  protected val expectations: Var[List[MessageHandler]] = Var(Nil)

  lazy val receive: MessageHandler = onmessage

}
*/