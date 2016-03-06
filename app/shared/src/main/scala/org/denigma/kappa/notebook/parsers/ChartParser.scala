package org.denigma.kappa.notebook.parsers

import fastparse.all._
import org.denigma.controls.charts.Point
import org.denigma.kappa.messages.KappaMessages
import org.denigma.kappa.messages.KappaMessages.{Chart, KappaSeries}

import scala.collection.immutable.Vector


case class ChartParser(lines: Vector[String]) {

  val quoted = P("'" ~ (!("'")~AnyChar).rep(1).! ~ "'")

  val spaced = P((" ").rep(0) ~ (!" " ~ AnyChar).rep(1).! ~ (" ").rep(0))

  val headerParser: P[scala.Seq[String]] = (quoted | spaced).rep(0)

  lazy val chart: Chart = if(lines.isEmpty) Chart.empty else {
    //if(lines.isEmpty) throw new Exception("chart is empty")
    val headerString = lines.head.replace("# time", "time").trim()
    val headers =  headerParser.parse(headerString).get.value.toList
    val data = lines.tail
    val titles = headers.tail
    val cols = headers.size
    val arr: Array[Array[Point]] = new Array[Array[Point]](cols-1) //does not include time
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
      if(values.length + 1 < cols) throw new Exception(s"Data row $row has less elements than headers")
      val time = values(0)
      for(c <- arr.indices){
        arr(c)(row) = Point(time, values(c+1))
      }
    }
    val series = arr.zipWithIndex.map{case (col, i) =>
      val points = col.toList
      KappaSeries(titles(i), points)
    }.toList
    KappaMessages.Chart(series)
  }

}