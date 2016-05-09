package org.denigma.kappa


import java.io.{File => JFile}
import java.nio.ByteBuffer

import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.BinaryMessage.Strict
import akka.http.scaladsl.testkit.WSProbe
import akka.util.ByteString
import better.files.File
import boopickle.DefaultBasic._
import net.ceedubs.ficus.Ficus._
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager
import org.denigma.kappa.notebook.communication.WebSocketManager
import org.denigma.kappa.notebook.pages.WebSockets

import scala.collection.immutable._
import scala.util.Success
abstract class BasicWebSocketSuite extends BasicKappaSuite with KappaPicklers {

  lazy val (host, port) = (config.getString("app.host"), config.getInt("app.port"))

  //val filePath: String = config.as[Option[String]]("app.files").getOrElse("files/")
  //val files = File(filePath)

  //files.createIfNotExists(asDirectory = true)
  //val fileManager = new FileManager(files)

  //val transport = new WebSocketManager(system, fileManager)

  //val routes = new WebSockets(transport.openChannel).routes

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

  def checkMessage[T](wsClient: WSProbe, projectToLoad: KappaProject)(partial: PartialFunction[KappaMessage, T]): T =
  {
    val bytes = Pickle.intoBytes[KappaMessage](FileRequests.Load(projectToLoad))
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
