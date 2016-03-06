package org.denigma.kappa.syntax
import ammonite.ops
import ammonite.ops._
import fastparse.all._
import org.denigma.controls.charts.Point
import org.denigma.kappa.messages.{KappaPicklers, KappaMessages}
import org.denigma.kappa.messages.KappaMessages.{Container, KappaSeries}
import scala.io.Source
import scala.util._
import scala.util.Try
import scala.collection.immutable._
import ammonite.ops.ImplicitWd._

trait KappaAgent

object Kappa extends KappaPicklers{

  def tempFolder(project: String = "kappa-notebook"):Path = {
    val dir = java.nio.file.Files.createTempDirectory(
      java.nio.file.Paths.get(System.getProperty("java.io.tmpdir")), project
    )
    Path(dir)
  }

  def writeFile(where: Path, name: String, strings: scala.Seq[String]) = {
    val path = where / name
    rm! path
    write( path, strings)
    path.toString()
  }

  val quoted = P("'" ~ (!("'")~AnyChar).rep(1).! ~ "'")

  val spaced = P((" ").rep(0) ~ (!" " ~ AnyChar).rep(1).! ~ (" ").rep(0))

  val headerParser: P[scala.Seq[String]] = (quoted | spaced).rep(0)

  def loadChart(path: Path): KappaMessages.Chart = {
    val lines: Vector[String] = read.lines ! path
    KappaMessages.Chart.parse(lines)
  }

  /**
    * Runs kappa with some parameters
 *
    * @param code Code to Run
    * @param parameters Parameters to run with
    * @return
    */
  def run(code: KappaMessages.Code, parameters: KappaMessages.RunParameters): Container = {
    val kaname = parameters.kaname
    val folder: Path = tempFolder()
    val outPutFolder = folder
    val modelPath = writeFile(folder, kaname, code.lines)
    //println(modelPath)
    val chartName = kaname.replace(".ka", ".out")
    //val res = parameters.events.map(Seq[Shellable]("e", _)).flatMap{value=>value++parameters.time.map(Seq[Shellable]("e", _))}
    //%%KaSim("-i", modelPath, "-e", ev,"-t",t,"-p", parameters.points, "-o", chartName, "-d", outPutFolder)
    val params: Seq[Shellable] = Seq[Shellable]("-i", modelPath)++
      Seq(parameters.events.map(e=>Seq[Shellable]("-e", e)), parameters.time.map(t=>Seq[Shellable]("-t", t))).flatten.flatten ++
      Seq[Shellable]("-p", parameters.points, "-o", chartName, "-d", outPutFolder) ++
      parameters.optional.map(value=>value:Shellable)
    //println(s"Params are ${params}")
    //println(s"Params are ${params.mkString(" | ")}")
    val result = Try( %%.applyDynamic("KaSim")(params:_*) )
    result match {
      case Success(command)=>
        val console = KappaMessages.Console(command.out.lines.toList)
        //val chart = loadChart(outPutFolder / chartName)
        val out = outPutFolder / chartName
        val lines = read.lines ! out
        val output = KappaMessages.Output(lines)
        KappaMessages.Container(List(console, output))
      case Failure(message) =>
        KappaMessages.Container(KappaMessages.Console(message.getMessage))
    }
  }


}
