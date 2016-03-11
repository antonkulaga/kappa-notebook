package org.denigma.kappa.notebook

import java.nio.ByteBuffer

import org.denigma.binding.extensions._
import org.denigma.controls.papers.Bookmark
import org.denigma.controls.sockets.{BinaryWebSocket, WebSocketSubscriber}
import org.denigma.kappa.WebSim
import org.denigma.kappa.WebSim.WebSimPicklers
import org.denigma.kappa.messages.{KappaChart}
import org.scalajs.dom
import org.scalajs.dom.raw.{ProgressEvent, FileReader, Blob, MessageEvent}
import rx.Var
import rx.Ctx.Owner.Unsafe.Unsafe
import io.circe._
import io.circe.generic.auto._
import io.circe.parser
import io.circe.syntax._
import cats.data.Xor
import shapeless.syntax._
import boopickle.Default._

import scala.collection.immutable._
import scala.scalajs.js.typedarray.{TypedArrayBuffer, ArrayBuffer}

object KappaHub{
  def empty: KappaHub = KappaHub(
    Var("HelloWorld.ka"),
    Var(WebSim.Defaults.code),
    Var(WebSim.Defaults.simulationStatus),
    Var(WebSim.Defaults.runModel)
  )
}

case class KappaHub(
  name: Var[String],
  code: Var[WebSim.Code],
  simulation: Var[WebSim.SimulationStatus],
  runParameters: Var[WebSim.RunModel],
  paperLocation: Var[Bookmark] = Var(Bookmark("", 0, Nil))
){
  val chart  = simulation.map{
    case s=> s.plot.map(KappaChart.fromKappaPlot).getOrElse(KappaChart.empty)
  }
  val console = simulation.map{
    case s=>
      //println("LOG:\n"+s.logMessages)
      s.logMessages.getOrElse("")
  }

  //val KappaChart = output.map(o => WebSim.Messages.KappaChart.parse(o))
}

case class WebSocketTransport(subscriber: WebSocketSubscriber, kappaHub: KappaHub) extends WebSimPicklers with BinaryWebSocket
{
  subscriber.onOpen.triggerLater{
    dom.console.log("WebSocket has been opened")
    //send(disc)
  }
/*
  def send(message: WebSim.WebSimMessage): Unit = {
    /*
    val txt: String = message.asJson.noSpaces
    println(message)
    println("SEND AS/n"+txt)
    subscriber.send(txt)
    */
  }

*/
  subscriber.onClose.triggerLater(
    dom.console.log("WebSocked has been closed")
  )

  subscriber.onMessage.onChange(onMessage)
  //chosen.onChange("chosenChange")(onChosenChange)
/*
  protected def receive(str: String) = {
    parser.decode[WebSim.WebSimMessage](str) match {
      case Xor.Left(failure)=> dom.console.error(s"JSON parsing failure detected $failure")
      case Xor.Right(value) => value match {
        case sm:WebSim.SimulationStatus => kappaHub.simulation() = sm
        case other => dom.console.error(s"UNKNOWN value:  $other")
      }

    }

  }

  override protected def onMessage(mess: MessageEvent) = {
    mess.data match{
      case str: String=>
        receive(str)
      //dom.console.error("NO STRING WEBSOCKET MESSAGE IS EXPECTED")

      case blob: Blob=>

        //dom.console.error("NO BINARY MESSAGE IS EXPECTED")
    }
  }
  */

  def send(message: WebSim.WebSimMessage): Unit = {
    val mes = bytes2message(Pickle.intoBytes(message))
    subscriber.send(mes)
  }

  subscriber.onClose.triggerLater(
    dom.console.log("WebSocked has been closed")
  )
  subscriber.onMessage.onChange(onMessage)
  //chosen.onChange("chosenChange")(onChosenChange)


  override protected def updateFromMessage(bytes: ByteBuffer): Unit = receive(Unpickle[WebSim.WebSimMessage].fromBytes(bytes))

  def receive: PartialFunction[WebSim.WebSimMessage, Unit] = {
    case message: WebSim.SimulationStatus =>
      println("UPDATE: \n"+message)
      kappaHub.simulation() = message
    case message: WebSim.Code => kappaHub.code() = message
    //case message: WebSim. => message.messages.foreach(receive)
    case other => dom.console.error(s"UNKNOWN KAPPA MESSAGE RECEIVED! "+other)

  }


}