package org.denigma.kappa.WebSim

import boopickle.Default._

import scala.List
import scala.collection.immutable._
import scala.concurrent.duration.FiniteDuration

trait WebSimPicklers {

    implicit val datePickler = transformPickler[java.util.Date, Long](_.getTime, t => new java.util.Date(t))

    object Single{

      implicit val messagePickler = compositePickler[WebSimMessage]//compositePickle[WebSimMessage]
        .addConcreteType[Code]
        .addConcreteType[Load]
        .addConcreteType[RunModel]
        .addConcreteType[Observable]
        .addConcreteType[KappaPlot]
        .addConcreteType[FluxData]
        .addConcreteType[FluxMap]
        .addConcreteType[SimulationStatus]
        .addConcreteType[VersionInfo]
        .addConcreteType[SimulationResult]
        .addConcreteType[SyntaxErrors]
    }

    import Single._

    implicit val kappaMessagePickler = compositePickler[WebSimMessage]
      .addConcreteType[Code]
      .addConcreteType[Load]
      .addConcreteType[RunModel]
      .addConcreteType[Observable]
      .addConcreteType[KappaPlot]
      .addConcreteType[FluxData]
      .addConcreteType[FluxMap]
      .addConcreteType[SimulationStatus]
      .addConcreteType[VersionInfo]
      .addConcreteType[SimulationResult]
      .addConcreteType[SyntaxErrors]
}


object Defaults
{
  lazy val runModel: RunModel = RunModel(code = "", max_events = Some(10000), max_time = None)
  lazy val simulationStatus: SimulationStatus = SimulationStatus(None, None, None, None, None, None, None,false, None, None, None/*, Array.empty[FluxMap]*/)
  lazy val code: Code = Code("")
}

sealed trait WebSimMessage

case class RunModel(code: String, nb_plot: Int = 250, max_events: Option[Int], max_time: Option[Double] = None) extends WebSimMessage

case class VersionInfo( build: String, version: String ) extends WebSimMessage

case class SimulationStatus(
                             time_percentage: Option[Double],
                             event: Option[Int],
                             event_percentage: Option[Double],
                             tracked_events: Option[Int],
                             nb_plot: Option[Int],
                             max_time: Option[Int],
                             max_events: Option[Int],
                             is_running: Boolean,
                             code: Option[String],
                             logMessages: Option[String],
                             plot: Option[KappaPlot]/*,
                             flux_maps: Array[FluxMap]*/
                           )  extends WebSimMessage
{
  def notFinished: Boolean = percentage < 100.0 && is_running//.getOrElse(true)

  def percentage: Double = event_percentage.orElse(time_percentage).get //showd throw if neither events not time are set
}

case class Observable(time: Double, values: Array[Double])  extends WebSimMessage

case class KappaPlot(legend: Array[String], observables: Array[Observable]) extends WebSimMessage {
  lazy val timePoints: List[Double] = observables.foldLeft(List.empty[Double])((acc, o)=> o.time::acc).reverse
}

case class FluxData(flux_name: String) extends WebSimMessage

case class FluxMap(flux_data: FluxData, flux_end: Double) extends WebSimMessage

object Code {
  def apply(lines: Seq[String]): Code = Code(lines.mkString("\n"))
}

case class Code(text: String) extends WebSimMessage
{
  def isEmpty = text == ""

  lazy val lines = text.split("\n").toList

  def withInsertion(num: Int, part: String): Code = withInsertion(num, part.split("\n").toList)

  def withInsertion(num: Int, newLines: Seq[String]): Code = if(num > lines.length)

    Code(lines.take(num) ++ newLines)  else Code(lines.take(num) ++ newLines ++ lines.drop(num))

}

case class Load(filename: String) extends WebSimMessage

trait ServerMessage extends WebSimMessage
{
  def server: String
}

//case class Run(username: String, server: String, message: WebSim.RunModel, userRef: ActorRef, interval: FiniteDuration) extends ServerMessage

case class SimulationResult(server: String, simulationStatus: SimulationStatus, token: Option[Int] = None, initialParams: Option[RunModel] = None) extends ServerMessage

case class SyntaxErrors(server: String, errors: Array[String], initialParams: Option[RunModel] = None) extends ServerMessage

