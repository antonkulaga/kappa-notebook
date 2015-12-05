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

case class WebSocketConnector(subscriber: WebSocketSubscriber) extends KappaPicklers with BinaryWebSocket
{
  subscriber.onOpen.handler{
    dom.console.log("IT OPENS!")
    //send(disc)
  }

  def send(message: KappaMessages.Message) = {
    val mes = bytes2message(Pickle.intoBytes(message))
    subscriber.send(mes)
  }

  subscriber.onClose.handler(
    dom.alert("CLOSED")
  )
  subscriber.onMessage.onChange("OnMessage",uniqueValue = false)(onMessage)
  //chosen.onChange("chosenChange")(onChosenChange)


  override protected def updateFromMessage(bytes: ByteBuffer): Unit = Unpickle[KappaMessages.Message].fromBytes(bytes) match
  {
      /*case KappaMessages.Discovered(devs,_,_)=>
        this.devices() = devs.map(Var(_))
        val d = chosen.now
        if(d.isEmpty || !devs.contains(d.get)){
          if(devs.nonEmpty) chosen() = Some(devs.head)
        }

      case KappaMessages.LastMeasurements(vals,channel,date)=>
        //println("VALUES RECEIVED "+vals)
        this.data() = vals
*/
        //if(chosen.now.contains())

      case other => dom.alert(s"MESSAGE RECEIVED! "+other)
  }
}