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
import org.denigma.kappa.messages.KappaMessage.Container
import org.denigma.kappa.messages.ProjectRequests.Remove
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
  val fileManager = new FileManager(files, log)

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
          val Container(ProjectResponses.ProjectList(lst)::(ProjectResponses.LoadedProject(proj))::Nil)  = checkTestProjects(wsClient)
          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Remove("big"))
          checkMessage(wsClient, rem){
            case Done(ProjectRequests.Remove(_), _) =>
          }
          println("removed message went well")
          checkProject(wsClient, big){
            case Failed(/*KappaProject("big", _, _)*/_, _, _) =>
          }
          println("remove is ok")
          val create: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Create(proj))
          checkMessage(wsClient, create){
            case Done(ProjectRequests.Create(_, false), _) =>
          }
          checkTestProjects(wsClient)
          println("create is ok")
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

          //create project
          val projectName = "crud"

          val s: File = files / projectName

          val folder = KappaFolder(s.pathAsString, files = SortedSet.empty)
          val crudProject = KappaProject(projectName, folder)

          val cr = ProjectRequests.Create(crudProject)
          val create: ByteBuffer = Pickle.intoBytes[KappaMessage](cr)
          checkMessage(wsClient, create){
            case org.denigma.kappa.messages.Done(_, _) =>
          }

          val Container(ProjectResponses.ProjectList(lst)::(ProjectResponses.LoadedProject(proj))::Nil)  = checkTestProjects(wsClient)
          val testName = "CRUD_Test.ka"

          val testFile = KappaFile("", testName, abc, saved = false)
          val fls = List(testFile)
          val sv: Save = FileRequests.Save(projectName = projectName, fls, rewrite = false )
          val save: ByteBuffer = Pickle.intoBytes[KappaMessage](sv)
          s.exists() shouldEqual true

          checkMessage(wsClient, save){
            case FileResponses.SavedFiles("crud", _) =>
          }

          val renames = Map(("CRUD_Test.ka", "CRUD.ka"), ("doesnotexist.ka", "also_does_not_exist.ka"))
          val rename = FileRequests.Rename(projectName = projectName, renames = renames, false)
          val rn: ByteBuffer = Pickle.intoBytes[KappaMessage](rename)
          checkMessage(wsClient, rn){
            case r: FileResponses.RenamingResult if r.notFound == Map(("doesnotexist.ka", "also_does_not_exist.ka")) /*&& r.renamed.keySet ==Set("CRUD_Test.ka")*/=>
          }

          val rv =  FileRequests.Remove(projectName = projectName, "CRUD.ka")
          val remove: ByteBuffer = Pickle.intoBytes[KappaMessage](rv)
          checkMessage(wsClient, remove){
            case Done(_, _) =>
          }
         val removed = FileRequests.Remove("crud", s.pathAsString)
         val remProj: Remove = ProjectRequests.Remove(projectName)
         val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](remProj)
         checkMessage(wsClient, rem){
           case org.denigma.kappa.messages.Done(_, _) =>
         }

        checkMessage(wsClient, Pickle.intoBytes[KappaMessage](ProjectRequests.GetList)){
          case ProjectResponses.ProjectList(l) if l.map(p=>p.name).toSet == Set("abc", "big")
           =>
        }
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
       val Container(ProjectResponses.ProjectList(lst)::(ProjectResponses.LoadedProject(proj))::Nil) = checkTestProjects(wsClient)

       val downloadWrong: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Download("big_wrong"))
       checkMessage(wsClient, downloadWrong){
         case Failed(_, List("project big_wrong does not exist"), _) =>
       }

       val downloadRight: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.Download("big"))
       val dat: Array[Byte] = checkMessage(wsClient, downloadRight){
         case FileResponses.Downloaded("big", data) =>

           val zp = fl.zip().byteArray
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
       checkTestProjects(wsClient)

       val Container(ProjectResponses.ProjectList(lst)::(ProjectResponses.LoadedProject(proj))::Nil)= checkTestProjects(wsClient)

       val paperName = "403339a0.pdf"
       val paper = files / ".." / "upload" / "403339a0.pdf"
       paper.exists() shouldEqual true


       val upload = FileRequests.UploadBinary("big", List(DataMessage(paperName, paper.byteArray)))
       val up: ByteBuffer = Pickle.intoBytes[KappaMessage](upload)
       wsClient.sendMessage(pack(up))


       checkProject(wsClient, big){
         case Container(ProjectResponses.ProjectList(l)::(ProjectResponses.LoadedProject(p))::Nil) =>
           p.name shouldEqual "big"
           p.folder.files.map(_.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka", paperName)
       }

       val load = FileRequests.LoadBinaryFile("big", paperName)
       val d = Pickle.intoBytes[KappaMessage](load)
       wsClient.sendMessage(pack(d))

       val dataList: List[Array[Byte]] = collectPartialKappaMessage(wsClient.inProbe, 4 seconds){
         case d @ DataChunk(_, _, data, downloaded, total, false) => data
       }{
         case d @ DataChunk(_, _, _, downloaded, total, true) =>
           true
       }

       val bytes = dataList.reduce(_ ++ _)
       bytes.sameElements(paper.byteArray) shouldEqual(true)

       val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](FileRequests.Remove("big", paperName))
       checkMessage(wsClient, rem){
         case Done(FileRequests.Remove(_, _), _) =>
       }
       checkTestProjects(wsClient)
     }
  }

}


def checkTestProjects(wsClient: WSProbe): Container = checkProject(wsClient, KappaProject("big")){
 case l @ Container(ProjectResponses.ProjectList(lst)::(ProjectResponses.LoadedProject(proj))::Nil) =>
   proj.name shouldEqual "big"
   proj.folder.files.map(_.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka")
   l
}

}