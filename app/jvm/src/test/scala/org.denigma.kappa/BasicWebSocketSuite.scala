package org.denigma.kappa


import java.io.{File => JFile}
import java.nio.ByteBuffer

import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.BinaryMessage.Strict
import akka.http.scaladsl.testkit.WSProbe
import akka.util.ByteString
import boopickle.DefaultBasic._
import org.denigma.kappa.messages._
abstract class BasicWebSocketSuite extends BasicKappaSuite with KappaPicklers {

  lazy val (host, port) = (config.getString("app.host"), config.getInt("app.port"))

  def pack(buffer:  ByteBuffer): Strict = BinaryMessage(ByteString(buffer))

  def checkMessage[T](wsClient: WSProbe, message: ByteBuffer)(partial: PartialFunction[KappaMessage, T]): T = {
    wsClient.sendMessage(pack(message))
    wsClient.inProbe.request(1).expectNextPF {
      case BinaryMessage.Strict(bytes) if {
        Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer) match {
          case l =>
            if(partial.isDefinedAt(l)) true else {
              println("checkProjects failed with: " + l)
              false
            }
        }
      } =>
        val value = Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer)
        partial(value)
    }
  }

  def checkProject[T](wsClient: WSProbe, projectToLoad: KappaProject)(partial: PartialFunction[KappaMessage, T]): T =
  {
    val bytes = Pickle.intoBytes[KappaMessage](ProjectRequests.Load(projectToLoad))
    checkMessage[T](wsClient, bytes)(partial)
  }


  def checkConnection(wsClient: WSProbe): Unit = {
    isWebSocketUpgrade shouldEqual true

    wsClient.inProbe.request(1).expectNextPF {
      case BinaryMessage.Strict(bytes) if {
        Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer) match {
          case c: Connected => true
          case _ => false
        }
      } =>
    }
  }

}
