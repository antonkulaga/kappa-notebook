package org.denigma.kappa

import better.files.File
import java.io.{File => JFile}
import java.nio.ByteBuffer

import scala.collection.immutable._
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.BinaryMessage.Strict
import akka.http.scaladsl.testkit.WSProbe
import akka.util.ByteString
import better.files.File
import boopickle.Default._
import net.ceedubs.ficus.Ficus._
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager
import org.denigma.kappa.notebook.communication.WebSocketManager
import org.denigma.kappa.notebook.pages.WebSockets
import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import better.files._
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import org.denigma.kappa.messages.{KappaFile, KappaFolder, KappaPath, KappaProject}

class FilesSuite extends BasicKappaSuite{

  val filePath: String = config.as[Option[String]]("app.files").getOrElse("files/")
  val files = File(filePath)
  files.createIfNotExists(asDirectory = true)
  val fileManager = new FileManager(files)

  "File manager" should {

    "load list of projects" in {
      val opt = config.as[Option[String]]("app.files")
      opt shouldEqual Some("test/files/")
      val projects = fileManager.loadProjectSet()
      val names = projects.map(_.name)
      names shouldEqual Set("abc", "big")
    }

    "load default project" in {
      val toLoad = KappaProject("big")
      //println("////////////////////////////////////////")
      println(config.as[Option[String]]("app.files"))
      println("path is " + fileManager.root.pathAsString)
      println("folders are " + fileManager.root.children.foldLeft("[")((acc, el)=>acc + " " + el.name) + "]")

      toLoad.loaded shouldEqual false
      val proj = fileManager.loadProject(toLoad)
      proj.folder.files.map(f=>f.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka")
      proj.loaded shouldEqual true
      val cont = proj.folder.files.collectFirst{
        case f if f.name.contains("big_0.ka")=> f.content
      }.get

      cont.contains("NA binding; ape:APE1 binding; xrc: XRCC1 binding") shouldBe true
    }

  }


}
