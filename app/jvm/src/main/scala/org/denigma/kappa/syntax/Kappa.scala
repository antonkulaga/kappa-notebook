package org.denigma.kappa.syntax
import ammonite.ops
import ammonite.ops._
import org.denigma.controls.charts.Point
import org.denigma.kappa.messages.{KappaPicklers, KappaMessages}
import org.denigma.kappa.messages.KappaMessages.KappaSeries
import scala.io.Source
import scala.util._
import scala.util.Try
import scala.collection.immutable._

trait KappaAgent

object Kappa extends KappaPicklers{

  lazy val folder = "/home/antonkulaga/CRI/compbio2/"

  lazy val file = folder+"KaSim"


  def tempFolder(project: String = "kappa-notebook"):Path = {
    val dir = java.nio.file.Files.createTempDirectory(
      java.nio.file.Paths.get(System.getProperty("java.io.tmpdir")), project
    )
    Path(dir)
  }

  implicit var wd = ops.Path(new java.io.File(folder))

  def writeFile(where: Path, name: String, strings: scala.Seq[String]) = {
    val path = wd/ name
    rm! path
    write( path, strings)
    path.toString()
  }


  def loadChart(path: Path): KappaMessages.Chart = {
    val lines: Vector[String] = read.lines ! path
    if(lines.isEmpty) throw new Exception("chart is empty")
    val (headers, data) = (lines.head.replace("# time", "time").split(" "), lines.tail)
    val titles = headers.tail
    val cols = headers.size -1
    if(cols < 0) throw new Exception("Too small chart file")
    val arr: Array[Array[Point]] = new Array[Array[Point]](cols)
    for(i <- arr.indices) arr(i) = new Array[Point](data.size)
    for(row <- data.indices)
    {
      val r = data(row).trim
      val values: Array[Double] = r.split(" ").map{str =>
        try str.toDouble catch {
          case exception: Throwable =>
            println(s"Cannot parse to Double following string:\n${str}\nwhole row is: \n${r}")
            throw exception
        }
      }
      val time = values(0)
      for(c <- arr.indices){
        arr(c)(row) = Point(time, values(c+1))
      }
    }
    val series = arr.zipWithIndex.map{case (col, i)=> KappaSeries(titles(i), col.toList) }.toList
    KappaMessages.Chart(series)
  }


  /**
    * Runs kappa with some parameters
    * @param code Code to Run
    * @param parameters Parameters to run with
    * @return
    */
  def run(code: KappaMessages.Code, parameters: KappaMessages.RunParameters): KappaMessages.Container = {
    val kaname = parameters.kaname
    val folder: Path = tempFolder()
    val outPutFolder = folder
    writeFile(folder, kaname, code.lines)
    val chartName = kaname.replace(".ka", ".out")
    val result: Try[CommandResult] = Try(
      (parameters.events, parameters.time) match { //bad code
      case (Some(ev), Some(t)) => %%KaSim("-i", kaname, "-e", ev,"-t",t,"-p", parameters.points, "-o", chartName, "-d", outPutFolder)
      case (Some(ev), None) =>  %%KaSim("-i", kaname, "-e", ev,"-p", parameters.points, "-o", chartName, "-d", outPutFolder)
      case (None, Some(t)) => %%KaSim("-i", kaname, "-t",t ,"-p", parameters.points, "-o", chartName, "-d", outPutFolder)
      case other => %%KaSim("-i", kaname,"-p", parameters.points, "-o", chartName, "-d", outPutFolder)
    })
    result match {
      case Success(command)=>
        val console = KappaMessages.Console(command.out.lines.toList)
        val chart = loadChart(outPutFolder / chartName)
        KappaMessages.Container(List(console, chart))
      case Failure(message) =>
        KappaMessages.Container(KappaMessages.Console(message.getMessage))
    }
  }


}
