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
import org.denigma.kappa.messages.FileResponses.{Downloaded, UploadStatus}
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
      WS("/channel/notebook?username=tester4", wsClient.flow) ~>  routes ~>
        check {
          // check response for WS Upgrade headers
          checkConnection(wsClient)
          val big = KappaProject("big")
          val ProjectResponses.Loaded(Some(proj), projects) = checkTestProjects(wsClient)
          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](FileRequests.Remove("big"))
          checkMessage(wsClient, rem){
            case Done(FileRequests.Remove(_), _) =>
          }
          checkProject(wsClient, big){
            case Failed(/*KappaProject("big", _, _)*/_, _, _) =>
          }
          val create: ByteBuffer = Pickle.intoBytes[KappaMessage](FileRequests.Create(proj))
          checkMessage(wsClient, create){
            case Done(FileRequests.Create(_, false), _) =>
          }
          checkTestProjects(wsClient)
          wsClient.sendCompletion()
          //wsClient.expectCompletion()
        }
    }

    "upload/remove files" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester5", wsClient.flow) ~>  routes ~>
        check {
          // check response for WS Upgrade headers
          checkConnection(wsClient)

          val fl = files / "big"
          fl.exists() shouldEqual true

          val big = KappaProject("big")
          val ProjectResponses.Loaded(Some(proj), projects) = checkTestProjects(wsClient)

          FileRequests.Upload

        }
    }

    "download, remove and upload project" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester5", wsClient.flow) ~>  routes ~>
        check {
          // check response for WS Upgrade headers
          checkConnection(wsClient)

          val fl = files / "big"
          fl.exists() shouldEqual true

          val big = KappaProject("big")
          val ProjectResponses.Loaded(Some(proj), projects) = checkTestProjects(wsClient)

          val downloadWrong: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Download("big_wrong"))
          checkMessage(wsClient, downloadWrong){
            case Failed(_,List("project big_wrong does not exist"), _) =>
          }
          val downloadRight: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Download("big"))
          val dat: Array[Byte] = checkMessage(wsClient, downloadRight){
            case Downloaded("big", data) =>
              val zp = fl.zip().byteArray
              //println("========================================")
              //println("data size = "+data.length)
              //println("zp   size = "+zp.length)
              data shouldEqual zp
              data
          }

          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](FileRequests.Remove("big"))
          checkMessage(wsClient, rem){
            case Done(FileRequests.Remove(_), _) =>
          }

          fl.exists() shouldEqual false

          checkProject(wsClient, big){
            case Failed(/*KappaProject("big", _, _)*/_, _, _) =>
          }

          val ms = DataMessage("big", dat)
          val upl = FileRequests.ZipUpload("big", ms, false )

          val upload: ByteBuffer = Pickle.intoBytes[KappaMessage](upl)
          //println("====================start upload of "+upload)
          checkMessage(wsClient, upload){
            case Done(upd: UploadStatus, _) =>
          }

          checkTestProjects(wsClient)
        }
    }

    "upload, streamed load and delete" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester6", wsClient.flow) ~>  routes ~>
        check {
          // check response for WS Upgrade headers
          checkConnection(wsClient)

          val big = KappaProject("big")
          checkProject(wsClient, big){
            case l @ ProjectResponses.Loaded(Some(p), _) =>
              //println("LOADED = "+ l)
              p.name shouldEqual "big"
              p.folder.files.map(_.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka")
              l
          }

          val ProjectResponses.Loaded(Some(proj), projects) = checkTestProjects(wsClient)

          val paperName = "403339a0.pdf"
          val paper = files / "upload" / "403339a0.pdf"
          paper.exists() shouldEqual true

          val upload = FileRequests.Upload("big", List(DataMessage(paperName, paper.byteArray)))
          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](FileRequests.Remove("big"))

          checkProject(wsClient, big){
            case l @ ProjectResponses.Loaded(Some(p), _) =>
              //println("LOADED = "+ l)
              p.name shouldEqual "big"
              p.folder.files.map(_.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka", paperName)
              l
          }

          val load = FileRequests.LoadFile("big", paperName)
          val downloadWrong: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Download("big_wrong"))
          checkMessage(wsClient, downloadWrong){
            case Failed(_,List("project big_wrong does not exist"), _) =>
          }





          /*
          val fl = files / "big"
          fl.exists() shouldEqual true

          val big = KappaProject("big")
          val ProjectResponses.Loaded(Some(proj), projects) = checkTestProjects(wsClient)

          val downloadWrong: ByteBuffer = Pickle.intoBytes[KappaMessage](FileRequests.Download("big_wrong"))
          checkMessage(wsClient, downloadWrong){
            case Failed(_,List("project big_wrong does not exist"), _) =>
          }
          val downloadRight: ByteBuffer = Pickle.intoBytes[KappaMessage](FileRequests.Download("big"))
          val dat: Array[Byte] = checkMessage(wsClient, downloadRight){
            case Downloaded("big", data) =>
              val zp = fl.zip().byteArray
              //println("========================================")
              //println("data size = "+data.length)
              //println("zp   size = "+zp.length)
              data shouldEqual zp
              data
          }

          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](FileRequests.Remove("big"))
          checkMessage(wsClient, rem){
            case Done(FileRequests.Remove(_), _) =>
          }

          fl.exists() shouldEqual false

          checkMessage(wsClient, big){
            case Failed(/*KappaProject("big", _, _)*/_, _, _) =>
          }

          val ms = DataMessage("big", dat)
          val upl = FileRequests.ZipUpload("big", ms, false )

          val upload: ByteBuffer = Pickle.intoBytes[KappaMessage](upl)
          //println("====================start upload of "+upload)
          checkMessage(wsClient, upload){
            case Done(upd: UploadStatus, _) =>
          }

          checkTestProjects(wsClient)
          */
        }
    }



  }

  def checkTestProjects(wsClient: WSProbe): ProjectResponses.Loaded = checkProject(wsClient, KappaProject("big")){
    case l @ ProjectResponses.Loaded(Some(proj), projects) =>
      //println("LOADED = "+ l)
      proj.name shouldEqual "big"
      proj.folder.files.map(_.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka")
      l
  }

}

