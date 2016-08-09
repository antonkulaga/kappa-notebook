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
import scala.collection.immutable.{Set, _}
import scala.concurrent.{Await, Future}

class WebSocketFilesSuite extends BasicWebSocketSuite {

  val filePath: String = config.as[Option[String]]("app.files").getOrElse("files/")
  val files = File(filePath)
  files.createIfNotExists(asDirectory = true)
  val fileManager = new FileManager(files, log)

  val transport = new WebSocketManager(system, fileManager)

  val routes = new WebSockets(transport.openChannel).routes

  "Via websocket we" should {

    "CRUD source files" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester5", wsClient.flow) ~> routes ~>
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
          checkMessage(wsClient, create) { case org.denigma.kappa.messages.Done(_, _) => }

          val Container(ProjectResponses.ProjectList(lst) :: (ProjectResponses.LoadedProject(proj)) :: Nil) = checkTestProjects(wsClient)
          val testName = "CRUD_Test.ka"

          val testFile = KappaSourceFile(projectName + "/" + testName, abc, saved = false)
          val fls = List(testFile)
          val sv: Save = FileRequests.Save(fls, rewrite = false)
          val save: ByteBuffer = Pickle.intoBytes[KappaMessage](sv)
          s.exists() shouldEqual true

          checkMessage(wsClient, save) { case FileResponses.SavedFiles(_) =>}

          val notFound = ("crud/doesnotexist.ka", "crud/also_does_not_exist.ka")
          val renames = Map(("crud/CRUD_Test.ka", "crud/CRUD.ka"), notFound)
          val rename = FileRequests.Rename(renames = renames, rewriteIfExists = false)
          val rn: ByteBuffer = Pickle.intoBytes[KappaMessage](rename)
          checkMessage(wsClient, rn) {
            case r: FileResponses.RenamingResult if r.notFound == Map(notFound) /*&& r.renamed.keySet ==Set("CRUD_Test.ka")*/ =>
          }

          val rv = FileRequests.Remove(Set("crud/CRUD.ka"))
          val remove: ByteBuffer = Pickle.intoBytes[KappaMessage](rv)
          checkMessage(wsClient, remove) { case Done(_, _) => }

          val removed = FileRequests.Remove(Set(s.pathAsString))
          val remProj: Remove = ProjectRequests.Remove(projectName)
          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](remProj)
          checkMessage(wsClient, rem) { case org.denigma.kappa.messages.Done(_, _) => }

          val msg: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.GetList)
          checkMessage(wsClient, msg) { case ProjectResponses.ProjectList(l) if l.map(p => p.name).toSet == Set("abc", "big") => }
        }
    }

    "upload binary files" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester5", wsClient.flow) ~> routes ~>
        check {
          // check response for WS Upgrade headers
          checkConnection(wsClient)

          val projectName = "upload"
          val fileName = "403339a0.pdf"
          val s: File = files.sibling("upload") / fileName
          val folder = KappaFolder((files / projectName).pathAsString, files = SortedSet.empty)
          val uploadProject = KappaProject(projectName, folder)
          val filePath = files / projectName / fileName

          val cr = ProjectRequests.Create(uploadProject)
          val create: ByteBuffer = Pickle.intoBytes[KappaMessage](cr)
          checkMessage(wsClient, create) {
            case org.denigma.kappa.messages.Done(_, _) =>
          }

          val bytes = ByteBuffer.wrap(s.loadBytes)
          val mess = KappaBinaryFile(filePath.pathAsString, bytes)
          val upl = FileRequests.Save(List(mess), true)
          val uplMess = Pickle.intoBytes[KappaMessage](upl)
          checkMessage(wsClient, uplMess) {
            case FileResponses.SavedFiles(Left(p::Nil)) if p == filePath.pathAsString =>
          }

          val remProj: Remove = ProjectRequests.Remove(projectName)
          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](remProj)
          checkMessage(wsClient, rem) { case org.denigma.kappa.messages.Done(_, _) => }

          val msg: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.GetList)
          checkMessage(wsClient, msg) { case ProjectResponses.ProjectList(l) if l.map(p => p.name).toSet == Set("abc", "big") => }
        }
    }
    /*

    "save folder" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester5", wsClient.flow) ~> routes ~>
        check {
          // check response for WS Upgrade headers
          checkConnection(wsClient)

          val projectName = "big"
          val folder: KappaFolder = fileManager.listFolder(File("abc"), fileManager.root).moveTo((files / "big").pathAsString, false)

          /*
          val cr = ProjectRequests.Create(uploadProject)
          val create: ByteBuffer = Pickle.intoBytes[KappaMessage](cr)
          checkMessage(wsClient, create) {
            case org.denigma.kappa.messages.Done(_, _) =>
          }

          val bytes = ByteBuffer.wrap(s.loadBytes)
          val mess = KappaBinaryFile(filePath.pathAsString, bytes)
          val upl = FileRequests.Save(List(mess), true)
          val uplMess = Pickle.intoBytes[KappaMessage](upl)
          checkMessage(wsClient, uplMess) {
            case FileResponses.SavedFiles(Left(p::Nil)) if p == filePath.pathAsString =>
          }

          val remProj: Remove = ProjectRequests.Remove(projectName)
          val rem: ByteBuffer = Pickle.intoBytes[KappaMessage](remProj)
          checkMessage(wsClient, rem) { case org.denigma.kappa.messages.Done(_, _) => }

          val msg: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectRequests.GetList)
          checkMessage(wsClient, msg) { case ProjectResponses.ProjectList(l) if l.map(p => p.name).toSet == Set("abc", "big") => }
        }
        */
    }
    */

  }

  def checkTestProjects(wsClient: WSProbe): Container = checkProject(wsClient, KappaProject("big")){
   case l @ Container(ProjectResponses.ProjectList(lst)::(ProjectResponses.LoadedProject(proj))::Nil) =>
     proj.name shouldEqual "big"
     proj.folder.files.map(_.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka")
     l
   }
}