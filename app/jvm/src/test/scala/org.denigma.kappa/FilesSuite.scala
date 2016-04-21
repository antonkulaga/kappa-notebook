package org.denigma.kappa

import better.files.File

import java.io.{File => JFile}
import java.nio.ByteBuffer

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

class FilesSuite extends BasicKappaSuite{

  val filePath: String = config.as[Option[String]]("app.files").getOrElse("files/")
  val files = File(filePath)
  files.createIfNotExists(asDirectory = true)
  val fileManager = new FileManager(files)

  "File manager" should {
    "load repressilator" in {
      val rep: String = fileManager.cd("repressilator").read("repress.ka")
      rep.contains("'tetR.degradation'") shouldEqual(true)
    }
  }


}
