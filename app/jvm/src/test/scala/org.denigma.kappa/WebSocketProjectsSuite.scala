package org.denigma.kappa

import java.io.{File => JFile}
import java.nio.ByteBuffer

import akka.http.scaladsl.testkit.WSProbe
import better.files.File
import boopickle.DefaultBasic._
import net.ceedubs.ficus.Ficus._
import org.denigma.kappa.messages.FileResponses.UploadStatus
import org.denigma.kappa.messages.KappaMessage.Container
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager
import org.denigma.kappa.notebook.communication.WebSocketManager
import org.denigma.kappa.notebook.pages.WebSockets

import scala.List
import scala.collection.immutable._

class WebSocketProjectsSuite extends BasicWebSocketSuite {

  val filePath: String = config.as[Option[String]]("app.files").getOrElse("files/")
  val files = File(filePath)
  files.createIfNotExists(asDirectory = true)
  val fileManager = new FileManager(files, log)

  val transport = new WebSocketManager(system, fileManager)

  val routes = new WebSockets(transport.openChannel).routes

  "Via websocket we" should {

    "load projects" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester3", wsClient.flow) ~> routes ~>
        check {
          checkConnection(wsClient)
          checkTestProjects(wsClient)
        }
      wsClient.sendCompletion()
      //wsClient.expectCompletion()
    }

    "update projects" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester4", wsClient.flow) ~> routes ~>
        check {
          // check response for WS Upgrade headers
          checkConnection(wsClient)
          val big = KappaProject("big")
          val Container(ProjectResponses.ProjectList(lst) :: (ProjectResponses.LoadedProject(proj)) :: Nil, _) = checkTestProjects(wsClient)
          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Remove("big"))
          checkMessage(wsClient, rem) {
            case Done(ProjectRequests.Remove(_), _) =>
          }
          println("removed message went well")
          checkProject(wsClient, big) {
            case Failed(/*KappaProject("big", _, _)*/ _, _, _) =>
          }
          println("remove is ok")
          val create: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Create(proj))
          checkMessage(wsClient, create) {
            case Done(ProjectRequests.Create(_, false), _) =>
          }
          checkTestProjects(wsClient)
          println("create is ok")
          wsClient.sendCompletion()
          //wsClient.expectCompletion()
        }
    }

    "download, remove and upload project" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester5", wsClient.flow) ~> routes ~>
        check {
          // check response for WS Upgrade headers
          checkConnection(wsClient)

          val fl = files / "big"
          fl.exists() shouldEqual true

          val big = KappaProject("big")
          val Container(ProjectResponses.ProjectList(lst) :: (ProjectResponses.LoadedProject(proj)) :: Nil, _) = checkTestProjects(wsClient)

          val downloadWrong: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Download("big_wrong"))
          checkMessage(wsClient, downloadWrong) {
            case Failed(_, List("project big_wrong does not exist"), _) =>
          }

          val downloadRight: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Download("big"))
          val dat: Array[Byte] = checkMessage(wsClient, downloadRight) {
            case FileResponses.Downloaded("big", data) =>
              val zp = fl.zip().byteArray
              data.sameElements(zp) shouldEqual true
              //data shouldEqual zp
              data
          }

          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Remove("big"))
          checkMessage(wsClient, rem) {
            case Done(ProjectRequests.Remove(_), _) =>
          }

          fl.exists() shouldEqual false

          checkProject(wsClient, big) {
            case Failed(/*KappaProject("big", _, _)*/ _, _, _) =>
          }

          val ms = KappaBinaryFile("big", ByteBuffer.wrap(dat))
          val upl = FileRequests.ZipUpload(ms, false)

          val upload: ByteBuffer = Pickle.intoBytes[KappaMessage](upl)

          checkMessage(wsClient, upload) {
            case Done(upd: UploadStatus, _) =>
          }

          checkTestProjects(wsClient)
        }
    }


    def checkTestProjects(wsClient: WSProbe): Container = checkProject(wsClient, KappaProject("big")) {
      case l@Container(ProjectResponses.ProjectList(lst) :: (ProjectResponses.LoadedProject(proj)) :: Nil, _) =>
        proj.name shouldEqual "big"
        proj.folder.files.map(_.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka")
        l
    }
  }
}