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
  val fileManager = new FileManager(files, log)

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
      toLoad.loaded shouldEqual false
      val projOpt: Option[KappaProject] = fileManager.loadProject(toLoad)
      projOpt.isDefined shouldEqual true
      val proj = projOpt.get
      proj.folder.files.map(f=>f.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka")
      println(proj.folder.files.mkString(" || "))
      proj.folder.files.map(f=>f.path) shouldEqual Set("big/big_0.ka", "big/big_1.ka", "big/big_2.ka")
      proj.loaded shouldEqual true
      val cont = proj.folder.files.collectFirst{
        case f if f.name.contains("big_0.ka")=> f.content
      }.get
      cont.contains("NA binding; ape:APE1 binding; xrc: XRCC1 binding") shouldBe true
    }

  }


}
