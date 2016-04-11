package org.denigma.kappa.notebook

import java.nio.ByteBuffer

import org.denigma.binding.extensions._
import org.denigma.codemirror.PositionLike
import org.denigma.controls.papers.Bookmark
import org.denigma.controls.sockets.{BinaryWebSocket, WebSocketSubscriber}
import org.scalajs.dom
import rx.Var
import rx.Ctx.Owner.Unsafe.Unsafe
import io.circe._
import io.circe.generic.auto._
import io.circe.parser
import io.circe.syntax._
import cats.data.Xor
import shapeless.syntax._
import boopickle.Default._
import org.denigma.kappa.WebSim
import org.denigma.kappa.WebSim.WebSimPicklers

import scala.collection.immutable._
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}


case class WebSocketTransport(subscriber: WebSocketSubscriber, kappaHub: KappaHub) extends WebSimPicklers with BinaryWebSocket
{
  subscriber.onOpen.triggerLater{
    dom.console.log("WebSocket has been opened")
    //send(disc)
  }

  def send(message: WebSim.WebSimMessage): Unit = {
    val mes = bytes2message(Pickle.intoBytes(message))
    subscriber.send(mes)
  }
  subscriber.onClose.triggerLater(
    dom.console.error("WebSocked has been closed")
  )

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
    receive(Unpickle[WebSim.WebSimMessage].fromBytes(bytes))
  }

  def receive: PartialFunction[WebSim.WebSimMessage, Unit] = {

    case WebSim.SyntaxErrors(server, errors, params) =>
      //println("CONSOLE: \n"+message.logMessages.getOrElse(""))

      kappaHub.errors() = errors.toList


    case WebSim.SimulationResult(server, status, token, params) =>
      //println("CONSOLE: \n"+message.logMessages.getOrElse(""))
      kappaHub.simulations() = kappaHub.simulations.now.updated((token, params.getOrElse(status.runParameters)), status)
      if(kappaHub.errors.now.nonEmpty) kappaHub.errors() = List.empty


    case message: WebSim.Code =>
      kappaHub.kappaCode() = message

    case message: WebSim.Connected =>
      send(WebSim.Load("model.ka"))

    //case message: WebSim. => message.messages.foreach(receive)
    case other =>
      dom.console.error(s"UNKNOWN KAPPA MESSAGE RECEIVED! "+other)

  }

}