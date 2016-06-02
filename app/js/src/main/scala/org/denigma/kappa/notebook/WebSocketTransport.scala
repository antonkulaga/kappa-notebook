package org.denigma.kappa.notebook

import java.nio.ByteBuffer

import boopickle.DefaultBasic._
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import org.denigma.controls.sockets._
import org.scalajs.dom
import org.scalajs.dom.raw.WebSocket
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.{Rx, Var}
import org.denigma.binding.extensions._
import org.denigma.kappa.messages.{Connected, Disconnected, EmptyKappaMessage, KappaMessage}
import rx.Rx.Dynamic
import rx.opmacros.Utils.Id

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

class Collecter[Input, Output]
  (input: Rx[Input])
  (collect: PartialFunction[Input, Output])
  (until: PartialFunction[Input, Boolean])
  {
    protected val promise = Promise[List[Output]]

    val collection: Rx[List[Output]] = input.fold(List.empty[Output]){
      case (acc, el)=>
        if(collect.isDefinedAt(el)) collect(el)::acc else acc
    }

    val inputObservable = input.triggerLater {
      val mes = input.now
      if (until.isDefinedAt(mes) && until(mes)) {
        val result = collection.now.reverse
        collection.kill()
        promise.success(result)
      }
    }

    lazy val future = promise.future
    future.onComplete{
      case _ =>
        inputObservable.kill()
    }
}

object WebSocketTransport {
  def apply(protocol: String, host: String, channel: String, username: String): WebSocketTransport = new  WebSocketTransport(protocol, host, channel, username)

  def apply(host: String, channel: String, username: String): WebSocketTransport = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    apply(wsProtocol, host, channel, username)
  }

  def apply(channel: String, username: String): WebSocketTransport = {
    val host = dom.document.location.host
    apply(host, channel, username)
  }
}

class WebSocketTransport(val protocol: String, val host: String, val channel: String, username: String) extends WebSocketTransport1
{

  type Input = KappaMessage


  override val connected = Var(false)

  input.triggerLater{
    onInput(input.now)
  }

  def collect[Output](partialFunction: PartialFunction[Input, Output])(until: PartialFunction[Input, Boolean]) = {
    new Collecter[Input, Output](input)(partialFunction)(until).future
  }
  /*
  def collect[Output](until: Input => Boolean)(partialFunction: PartialFunction[Input, Output]) = {
    new Collecter[Input, Output](input, until)(partialFunction).future
  }

  dellect[Result](message: Output, until: Input=> Boolean)(zero: Result)(foldLeft: PartialFunction): Future[Result] = {
    //println("ask is used for message "+message)
    val expectation = TimeoutExpectation[Input, Result](input, timeout)(partial)
    output() = message
    expectation.future
  }
  */

  protected def onInput(inp: Input) = inp match {
    case Connected(uname, ch, list, servers) if uname==username /*&& ch == channel*/ =>
      println(s"connection of user $username to $channel established")
      connected() = true
    case Disconnected(uname, ch, list) if uname==username /* && ch == channel */ =>
      println(s"user $username diconnected from $channel")
      connected() = false

    case _=> //do nothing
  }

  override def send(message: Output): Unit = if(connected.now) {
    val mes = bytes2message(pickle(message))
    send(mes)
  } else {
    connected.triggerOnce{
      case true =>
        send(message)
      case false =>
    }
  }


  override protected def closeHandler() = {
    println("websocket closed")
    connected() = false
    opened() = false
  }

  override def getWebSocketUri(username: String): String = {
    s"$protocol://$host/channel/$channel?username=$username"
  }

  def open(): Unit = {
    urlOpt() = Option(getWebSocketUri(username))
  }

  override def initWebSocket(url: String): WebSocket = WebSocketStorage(url)

  override def emptyInput: KappaMessage = EmptyKappaMessage

  override protected def pickle(message: Output): ByteBuffer = {
    Pickle.intoBytes(message)
  }

  override protected def unpickle(bytes: ByteBuffer): KappaMessage = {
    Unpickle[Input].fromBytes(bytes)
  }
}
/*
import java.nio.ByteBuffer

import boopickle.DefaultBasic._
import org.denigma.controls.sockets.{BinaryWebSocket, WebSocketStorage, WebSocketSubscriber}
import org.denigma.kappa.messages._
//import org.denigma.kappa.notebook.storage.WebSocketStorage
import org.scalajs.dom
import org.scalajs.dom.raw.WebSocket
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Var

case class WebSocketTransport(channel: String, username: String) extends KappaPicklers with BinaryWebSocket with WebSocketSubscriber
{

override def getWebSocketUri(username: String): String = {
  val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
  s"$wsProtocol://${dom.document.location.host}/channel/$channel?username=$username"
}

def open() = {
  urlOpt() = Option(getWebSocketUri(username))
}

onKappaMessage.triggerLater{
  val mess = onKappaMessage.now
  onKappaMessage(mess)
}


override protected def updateFromKappaMessage(bytes: ByteBuffer): Unit = {
  //println("from bytes fires")
  val message: KappaKappaMessage = Unpickle[KappaKappaMessage].fromBytes(bytes)
  //expectations() = updatedExpectations(message)
  onKappaKappaMessage() = message
}

val onKappaKappaMessage: Var[KappaKappaMessage] = Var(EmptyKappaKappaMessage)
val sendKappaKappaMessage: Var[KappaKappaMessage] = Var(EmptyKappaKappaMessage)
sendKappaKappaMessage.triggerLater{
  send(sendKappaKappaMessage.now)
}

def send(message: KappaKappaMessage): Unit = {
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
lazy val onKappaMessage: rx.Var[dom.KappaMessageEvent] = Var(Events.createKappaMessageEvent())
lazy val onError: rx.Var[dom.ErrorEvent] = Var(Events.createErrorEvent())
lazy val onClose: rx.Var[Event] = Var(Events.createEvent())
*/
override def initWebSocket(url: String): WebSocket = WebSocketStorage(url)
}
*/