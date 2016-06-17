package org.denigma.kappa.messages

import boopickle.CompositePickler

import scala.List
import scala.collection.immutable._


object WebSimMessage {
  import boopickle.DefaultBasic._

  implicit val webSimMessagePickler: CompositePickler[WebSimMessage] = compositePickler[WebSimMessage]
    .addConcreteType[Version]
    .addConcreteType[Location]
    .addConcreteType[WebSimRange]
    .addConcreteType[WebSimError]
    .addConcreteType[Location]
    .addConcreteType[WebSimNode]
    .addConcreteType[WebSimSide]
    .addConcreteType[ParseCode]
    .addConcreteType[ContactMap]
    .addConcreteType[Observable]
    .addConcreteType[KappaPlot]
    .addConcreteType[RunModel]
    .addConcreteType[FluxData]
    .addConcreteType[FluxMap]
    .addConcreteType[AgentState]
    .addConcreteType[TokenState]
    .addConcreteType[Snapshot]
    //.addConcreteType[UnaryDistances]
    .addConcreteType[SimulationStatus]

}

trait WebSimMessage

object Version {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[Version] = boopickle.Default.generatePickler[Version]
}
case class Version(build: String, version: String ) extends WebSimMessage

object Location {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[Location] = boopickle.Default.generatePickler[Location]
}

case class Location(line: Int, chr: Int) extends WebSimMessage

object WebSimRange {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[WebSimRange] = boopickle.Default.generatePickler[WebSimRange]
}
case class WebSimRange(file: String, from_position: Location, to_position: Location) extends WebSimMessage


object WebSimError {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[WebSimError] = boopickle.Default.generatePickler[WebSimError]
}

case class WebSimError(severity: String, message: String, range: WebSimRange) extends WebSimMessage

object WebSimNode {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[WebSimNode] = boopickle.Default.generatePickler[WebSimNode]
}

case class WebSimNode(node_name: String, node_sites: List[WebSimSide]) extends WebSimMessage

object WebSimSide {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[WebSimSide] = boopickle.Default.generatePickler[WebSimSide]
}

case class WebSimSide(site_name: String, site_links: List[(Int, Int)], site_states: List[String]) extends WebSimMessage

object ContactMap {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[ContactMap] = boopickle.Default.generatePickler[ContactMap]
}

case class ContactMap(contact_map: List[WebSimNode]) extends WebSimMessage

object ParseCode {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[ParseCode] = boopickle.Default.generatePickler[ParseCode]
}

case class ParseCode(code: String) extends WebSimMessage

//case class Parameter(code: String, nb_plot: Int, max_time: Double) extends WebSimMessage
object RunModel {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[RunModel] = boopickle.Default.generatePickler[RunModel]
}
case class RunModel(code: String, nb_plot: Option[Int] = Some(250), max_events: Option[Int], max_time: Option[Double] = None) extends WebSimMessage

object Observable {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[Observable] = boopickle.Default.generatePickler[Observable]
}
case class Observable(time: Double, values: List[Double])  extends WebSimMessage

object KappaPlot {
  lazy val empty = KappaPlot(Nil, Nil)
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[KappaPlot] = boopickle.Default.generatePickler[KappaPlot]
}
case class KappaPlot(legend: List[String], observables: List[Observable]) extends WebSimMessage {
  //println("kappa plot: "+legend.toList)
  lazy val timePoints: List[Double] = observables.foldLeft(List.empty[Double])((acc, o)=> o.time::acc).reverse
}

object FluxData {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[FluxData] = boopickle.Default.generatePickler[FluxData]
}

case class FluxData(flux_name: String, flux_start: List[Int]) extends WebSimMessage

object FluxMap {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[FluxMap] = boopickle.Default.generatePickler[FluxMap]
}

case class FluxMap(flux_rules: List[String], flux_data: FluxData, flux_end: Double) extends WebSimMessage

object AgentState {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[AgentState] = boopickle.Default.generatePickler[AgentState]
}

case class AgentState(quantity: Int, mixture: List[WebSimNode]) extends WebSimMessage

object TokenState {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[TokenState] = boopickle.Default.generatePickler[TokenState]
}

case class TokenState(token: String, value: Double) extends WebSimMessage

object Snapshot {
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[Snapshot] = boopickle.Default.generatePickler[Snapshot]
}

case class Snapshot(snap_file: String, snap_event: Int, agents: List[AgentState], tokens: List[TokenState]) extends WebSimMessage


object SimulationStatus {
  lazy val empty = SimulationStatus(0.0,
    None, None, None, None, None, None, None, is_running = false, None , Nil, None, Nil, Nil//, Nil
  )
  import boopickle.Default._
  implicit val classPickler: Pickler[SimulationStatus] = boopickle.Default.generatePickler[SimulationStatus]
}
case class SimulationStatus(
                             time: Double,
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
                             plot: Option[KappaPlot],
                             //snapshots: List[Snapshot],
                             flux_maps: List[FluxMap],
                             files: List[String]
                           )  extends WebSimMessage
{
  def notFinished: Boolean = percentage < 100.0 && is_running//.getOrElse(true)

  def percentage: Double = event_percentage.orElse(time_percentage).get //showd throw if neither events not time are set

  def runParameters: RunModel = RunModel(code.getOrElse(""), nb_plot, max_events, max_time)
}