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

import scala.collection.immutable._
import scala.util.Success

class WebSocketFilesSuite extends BasicWebSocketSuite {

  val filePath: String = config.as[Option[String]]("app.files").getOrElse("files/")
  val files = File(filePath)
  files.createIfNotExists(asDirectory = true)
  val fileManager = new FileManager(files)

  val transport = new WebSocketManager(system, fileManager)

  val routes = new WebSockets(transport.openChannel).routes

  "Via websocket we" should {

    "load projects" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester3", wsClient.flow) ~>  routes ~>
        check {
          checkConnection(wsClient)
          checkTestProjects(wsClient)

        }
      wsClient.sendCompletion()
      //wsClient.expectCompletion()
    }


    "update projects" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester3", wsClient.flow) ~>  routes ~>
        check {
          // check response for WS Upgrade headers
          checkConnection(wsClient)
          val big = KappaProject("big")
          val Loaded(proj :: two :: Nil) = checkTestProjects(wsClient)
          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](Remove("big"))
          checkMessage(wsClient, rem){
            case Done(Remove(_), _) =>
          }
          checkMessage(wsClient, big){
            case Failed(/*KappaProject("big", _, _)*/_, _, _) =>
          }
          val create: ByteBuffer = Pickle.intoBytes[KappaMessage](Create(proj))
          checkMessage(wsClient, create){
            case Done(Create(_, false), _) =>
          }
          checkTestProjects(wsClient)
          wsClient.sendCompletion()
          //wsClient.expectCompletion()
        }
    }


  }

  def checkTestProjects(wsClient: WSProbe): Loaded = checkMessage(wsClient, KappaProject("big")){
    case l @ Loaded(proj :: two :: Nil) =>
      //println("LOADED = "+ l)
      proj.name shouldEqual "big"
      proj.folder.files.map(_.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka")
      l
  }

}

