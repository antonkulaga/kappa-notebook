package org.denigma.kappa.messages

import scala.List
import scala.collection.immutable._


sealed trait WebSimMessage

case class RunModel(code: String, nb_plot: Option[Int] = Some(250), max_events: Option[Int], max_time: Option[Double] = None) extends WebSimMessage

case class VersionInfo( build: String, version: String ) extends WebSimMessage


object SimulationStatus {
  lazy val empty = SimulationStatus(
    None, None, None, None, None, None, None, is_running = false, None, Nil, None
  )
}
case class SimulationStatus(
                             time_percentage: Option[Double],
                             event: Option[Int],
                             event_percentage: Option[Double],
                             tracked_events: Option[Int],
                             nb_plot: Option[Int],
                             max_time: Option[Double],
                             max_events: Option[Int],
                             is_running: Boolean,
                             code: Option[String],
                             log_messages: List[String],
                             plot: Option[KappaPlot]
                             /*
                             debugJSON: Option[String] = None
                             ,
                             flux_maps: Array[FluxMap]*/
                           )  extends WebSimMessage
{
  def notFinished: Boolean = percentage < 100.0 && is_running//.getOrElse(true)

  def percentage: Double = event_percentage.orElse(time_percentage).get //showd throw if neither events not time are set

  def runParameters: RunModel = RunModel(code.getOrElse("### no code info"), nb_plot, max_events, max_time)
}

case class Observable(time: Double, values: List[Double])  extends WebSimMessage


object KappaPlot {
  lazy val empty = KappaPlot(Nil, Nil)
}
case class KappaPlot(legend: List[String], observables: List[Observable]) extends WebSimMessage {
  //println("kappa plot: "+legend.toList)
  lazy val timePoints: List[Double] = observables.foldLeft(List.empty[Double])((acc, o)=> o.time::acc).reverse
}

case class FluxData(flux_name: String) extends WebSimMessage

case class FluxMap(flux_data: FluxData, flux_end: Double) extends WebSimMessage


