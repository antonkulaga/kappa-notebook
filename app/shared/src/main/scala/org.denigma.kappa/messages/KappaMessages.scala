package org.denigma.kappa.messages

import boopickle.Default._
import org.denigma.controls.charts.{LineStyles, Point, Series}


sealed trait KappaMessage

import scala.collection.immutable._

object KappaChart {
  lazy val empty = KappaChart(List.empty)

  implicit def fromKappaPlot(plot: KappaPlot): KappaChart = {
    val series = plot.legend.zipWithIndex.map{ case (title, i) =>
      //println("title: " + title)
      KappaSeries(title, plot.observables.map(o=> Point(o.time, o.values(i))).toList) }
    KappaChart(series.toList)
  }
}


case class KappaChart(series: List[KappaSeries])
{
  def isEmpty: Boolean = series.isEmpty
}

object KappaSeries {

  import scala.util.Random

  def randomColor() = s"rgb(${Random.nextInt(255)},${Random.nextInt(255)},${Random.nextInt(255)})"

  def randomLineStyle() = LineStyles(randomColor(), 4 ,"none" , 1.0)

}


case class KappaSeries(title: String, points: List[Point], style: LineStyles = KappaSeries.randomLineStyle()) extends Series

//sealed trait WebSocketMessage


case class Connected(username: String, channel: String /*, time: LocalDateTime = LocalDateTime.now()*/) extends KappaMessage

case class Disconnected(username: String, channel: String /*, time: LocalDateTime = LocalDateTime.now()*/) extends KappaMessage

trait ServerMessage extends KappaMessage
{
  def server: String
}

//case class Run(username: String, server: String, message: WebSim.RunModel, userRef: ActorRef, interval: FiniteDuration) extends ServerMessage

case class SimulationResult(server: String, simulationStatus: SimulationStatus, token:Int, initialParams: Option[RunModel] = None) extends ServerMessage

case class SyntaxErrors(server: String, errors: Array[String], initialParams: Option[RunModel] = None) extends ServerMessage

case class LaunchModel(server: String, parameters: RunModel) extends ServerMessage


object Code {
  def apply(lines: Seq[String]): Code = Code(lines.mkString("\n"))
}

case class Code(text: String) extends KappaMessage
{
  def isEmpty = text == ""

  lazy val lines = text.split("\n").toList

  def withInsertion(num: Int, part: String): Code = withInsertion(num, part.split("\n").toList)

  def withInsertion(num: Int, newLines: Seq[String]): Code = if(num > lines.length)

    Code(lines.take(num) ++ newLines)  else Code(lines.take(num) ++ newLines ++ lines.drop(num))

}

case class Load(project: KappaProject = KappaProject.default)  extends KappaMessage


import scala.collection.immutable.{List, Nil}

object KappaProject {
  lazy val default = KappaProject("_default")
}

case class KappaProject(name: String) extends KappaMessage


object KappaPath{
  lazy val empty = KappaPath("", Nil, None, active = false)
}

case class KappaPath(path: String, children: List[KappaPath] = Nil, parent: Option[KappaPath] = None, active: Boolean = false) extends KappaMessage
