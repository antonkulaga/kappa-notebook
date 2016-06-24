package org.denigma.kappa

import java.io.{File => JFile}
import java.nio.ByteBuffer

import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.testkit.WSProbe
import akka.stream.testkit.TestSubscriber
import better.files.File
import boopickle.DefaultBasic._
import net.ceedubs.ficus.Ficus._
import org.denigma.kappa.messages.FileRequests.Save
import org.denigma.kappa.messages.FileResponses.{Downloaded, UploadStatus}
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager
import org.denigma.kappa.notebook.communication.WebSocketManager
import org.denigma.kappa.notebook.pages.WebSockets

import scala.List
import scala.concurrent.duration._
import scala.collection.immutable._
import scala.concurrent.{Await, Future}

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
          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Remove("big"))
          checkMessage(wsClient, rem){
            case Done(ProjectRequests.Remove(_), _) =>
          }
          checkProject(wsClient, big){
            case Failed(/*KappaProject("big", _, _)*/_, _, _) =>
          }
          val create: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Create(proj))
          checkMessage(wsClient, create){
            case Done(ProjectRequests.Create(_, false), _) =>
          }
          checkTestProjects(wsClient)
          wsClient.sendCompletion()
          //wsClient.expectCompletion()
        }
    }

    "CRUD source files" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester5", wsClient.flow) ~>  routes ~>
        check {
          // check response for WS Upgrade headers
          checkConnection(wsClient)

          val fl = files / "big"
          fl.exists() shouldEqual true

          val big = KappaProject("big")
          val ProjectResponses.Loaded(Some(proj), projects) = checkTestProjects(wsClient)
          //val abc = files / "abc" / "abc.ka"
          val testName = "CRUD_Test.ka"

          val testFile = KappaFile("", testName, abc, saved = false)
          val fls = List(testFile)
          val sv: Save = FileRequests.Save(projectName = big.name, fls, rewrite = false )
          val save: ByteBuffer = Pickle.intoBytes[KappaMessage](sv)
          /*
          checkMessage(wsClient, save){
            /*case FileResponses.FileSaved(_, saved) if saved == fls.map(v=>v.name).toSet =>
              println("FileResponses.FileSaved " + saved)
              */
            case other =>
              println("*************************************************************************")
              println(other)
          }
          //val sv: Save = FileRequests.Save(projectName = big.name, fls, rewrite = false )
          val rv = FileRequests.Remove(projectName = big.name, testName)
          val remove: ByteBuffer = Pickle.intoBytes[KappaMessage](rv)
          checkMessage(wsClient, remove){
            /*case FileResponses.FileSaved(_, saved) if saved == fls.map(v=>v.name).toSet =>
              println("FileResponses.FileSaved " + saved)
              */
            case other =>
              println("///////////////////////////////////////////////////////////////////////////")
              println(other)
          }
          */
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
            case Failed(_, List("project big_wrong does not exist"), _) =>
          }

          val downloadRight: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Download("big"))
          val dat: Array[Byte] = checkMessage(wsClient, downloadRight){
            case FileResponses.Downloaded("big", data) =>

              val zp = fl.zip().byteArray
              //val some = fileManager.loadZiped("big").get
              //val zp2 = fileManager.loadZiped("big").get.data
              //println("========================================")
              //println("SOMETHING RECEIVED "+smt)
              //println("data size = "+data.length)
              //println("zp   size = "+zp.length)
              //data.sameElements(zp) shouldEqual true
              data.sameElements(zp) shouldEqual true
              //data shouldEqual zp
              data
          }

          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Remove("big"))
          checkMessage(wsClient, rem){
            case Done(ProjectRequests.Remove(_), _) =>
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
          val paper = files / ".." / "upload" / "403339a0.pdf"
          paper.exists() shouldEqual true

          val upload = FileRequests.UploadBinary("big", List(DataMessage(paperName, paper.byteArray)))
          val up: ByteBuffer = Pickle.intoBytes[KappaMessage](upload)
          wsClient.sendMessage(pack(up))


          checkProject(wsClient, big){
            case l @ ProjectResponses.Loaded(Some(p), _) =>
              //println("LOADED = "+ l)
              p.name shouldEqual "big"
              p.folder.files.map(_.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka", paperName)
              l
          }

          val load = FileRequests.LoadBinaryFile("big", paperName)
          val d = Pickle.intoBytes[KappaMessage](load)
          wsClient.sendMessage(pack(d))

          val dataList: List[Array[Byte]] = collectPartialKappaMessage(wsClient.inProbe, 4 seconds){
            case d @ DataChunk(_, _, data, downloaded, total, false) => data
          }{
            case d @ DataChunk(_, _, _, downloaded, total, true) =>
              //println("COMPLETED, BYTES TOTAL ARE "+total)
              true
          }

          val bytes = dataList.reduce(_ ++ _)
          bytes.sameElements(paper.byteArray) shouldEqual(true)

          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](FileRequests.Remove("big", paperName))
          checkMessage(wsClient, rem){
            case Done(FileRequests.Remove(_, _), _) =>
          }

          checkProject(wsClient, big){
            case l @ ProjectResponses.Loaded(Some(p), _) =>
              //println("LOADED = "+ l)
              p.name shouldEqual "big"
              p.folder.files.map(_.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka")
              l
          }
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