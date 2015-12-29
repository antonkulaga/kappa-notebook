package org.denigma.kappa.syntax

import java.io.InputStream

import ammonite.ops
import ammonite.ops._
import org.denigma.kappa.messages.KappaMessages
import scala.io.Source

trait KappaAgent

case class KappaResult(command: CommandResult, series: Seq[String])
{
  lazy val output = command.out.lines
}


object Kappa {

  lazy val folder = "/home/antonkulaga/CRI/compbio2/"

  lazy val file = folder+"KaSim"

  //var outPutFolder = folder

  lazy val abc = {
    val stream : InputStream = getClass.getResourceAsStream("/examples/abc.ka")
    scala.io.Source.fromInputStream( stream ).getLines.toList
  }

  def tempFolder(project: String = "kappa-notebook"):Path = {
    val dir = java.nio.file.Files.createTempDirectory(
      java.nio.file.Paths.get(System.getProperty("java.io.tmpdir")), project
    )
    Path(dir)
  }

  implicit var wd = ops.Path(new java.io.File(folder))


  def writeFile(where: Path, name: String, strings: Seq[String]) = {
    val path = wd/ name
    rm! path
    write( path, strings)
    path.toString()
  }



  def run(code: KappaMessages.Code, parameters: KappaMessages.RunParameters) = {
    val kaname = parameters.kaname
    val chart = kaname.replace(".ka", ".out")
    val folder: Path = tempFolder()
    val outPutFolder = folder
    writeFile(folder, kaname, code.lines)
    import parameters._
    val command: CommandResult = %%KaSim("-i", kaname, "-e", maxEvents,"-p", points, "-o", chart, "-d", outPutFolder)
    //val ch: List[String] = Source.fromFile(folder + chart).getLines().toList
    val ch: Vector[String] = read.lines ! folder / chart
    KappaResult(command, ch)
  }


}
