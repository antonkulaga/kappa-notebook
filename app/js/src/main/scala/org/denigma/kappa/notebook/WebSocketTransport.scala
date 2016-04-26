package org.denigma.kappa.notebook

import java.nio.ByteBuffer

import boopickle.Default._
import org.denigma.binding.extensions._
import org.denigma.controls.sockets.{BinaryWebSocket, WebSocketSubscriber}
import org.denigma.kappa.messages._
import org.scalajs.dom
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Var

import scala.collection.immutable._
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js.timers


case class WebSocketTransport(subscriber: WebSocketSubscriber, kappaHub: KappaHub) extends KappaPicklers with BinaryWebSocket
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
        kappaHub.errors() = List(errorMessage)
      }
    }
    send(message)
  }

  subscriber.onClose.triggerLater {
    val message = "Websocket server connection has been closed"
    dom.console.error(message)
    kappaHub.errors() = message::kappaHub.errors.now
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

  def receive: MessageHandler = {

    case SyntaxErrors(server, errors, params) =>
      //println("CONSOLE: \n"+message.logMessages.getOrElse(""))

      kappaHub.errors() = errors


    case SimulationResult(server, status, token, params) =>
      //println("CONSOLE: \n"+message.logMessages.getOrElse(""))
      kappaHub.simulations() = kappaHub.simulations.now.updated((token, params.getOrElse(status.runParameters)), status)
      if(kappaHub.errors.now.nonEmpty) kappaHub.errors() = List.empty


    case message: Code =>
      kappaHub.kappaCode() = message

    case message: Connected =>
      //case message: WebSim. => message.messages.foreach(receive)

    case ServerErrors(errors) => kappaHub.errors() = errors

    case Loaded(project: KappaProject, other) =>
    //kappaHub.errors() = errors

    //case message: WebSim. => message.messages.foreach(receive)

    case other =>
      dom.console.error(s"UNKNOWN KAPPA MESSAGE RECEIVED! "+other)

  }

}