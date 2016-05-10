package org.denigma.kappa

import java.io.{File => JFile}

import better.files.File
import net.ceedubs.ficus.Ficus._
import org.denigma.kappa.messages.KappaProject
import org.denigma.kappa.notebook.FileManager

import scala.collection.immutable._

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
      //println(config.as[Option[String]]("app.files"))
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
