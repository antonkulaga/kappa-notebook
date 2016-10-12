package org.denigma.kappa.messages

import boopickle.CompositePickler
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{KappaSnapshot, Pattern, Site}

import scala.List
import scala.Predef.Set
import scala.collection.immutable._

object WebSimMessages {

  object WebSimMessage {
    import boopickle.DefaultBasic._

    implicit val webSimMessagePickler: CompositePickler[WebSimMessage] = compositePickler[WebSimMessage]
      .addConcreteType[Version]
      .addConcreteType[Location]
      .addConcreteType[WebSimRange]
      .addConcreteType[WebSimError]
      .addConcreteType[Location]
      .addConcreteType[WebSimNode]
      .addConcreteType[WebSimSite]
      .addConcreteType[ParseCode]
      .addConcreteType[ContactMap]
      .addConcreteType[Observable]
      .addConcreteType[KappaPlot]
      .addConcreteType[RunModel]
      .addConcreteType[FluxMap]
      .addConcreteType[AgentState]
      .addConcreteType[TokenState]
      .addConcreteType[Snapshot]
      .addConcreteType[FileLine]
      .addConcreteType[UnaryDistance]
      .addConcreteType[SimulationStatus]

  }

  trait WebSimMessage

  object Version {
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[Version] = boopickle.Default.generatePickler[Version]
  }
  case class Version(version_build: String, version_id: String ) extends WebSimMessage

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
  {
    lazy val fullMessage = s"[$severity] $message :${range.from_position.line}:${range.from_position.chr}-${range.to_position.line}:${range.to_position.chr}"
  }

  object WebSimNode {
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[WebSimNode] = boopickle.Default.generatePickler[WebSimNode]

    implicit def fromAgent(agent: KappaModel.Agent): WebSimNode ={
      val agentSites = agent.sites.map(site=>site: WebSimSite).toList
      WebSimNode(agent.name, agentSites)
    }


  }

  case class WebSimNode(node_name: String, node_sites: List[WebSimSite]) extends WebSimMessage
  {
    lazy val toAgent: KappaModel.Agent = {
      val sites = node_sites.map(s=>s.toSite).toSet
      KappaModel.Agent(node_name, sites)
    }
  }

  object WebSimSite {
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[WebSimSite] = boopickle.Default.generatePickler[WebSimSite]


    implicit def fromSite(site: Site): WebSimSite = {
      val links = site.links.map(l => (0, Integer.parseInt(l))).toList
      val states = site.states.map(s=>s.name).toList
      WebSimSite(site.name, links, states)
    }
  }

  case class WebSimSite(site_name: String, site_links: List[(Int, Int)], site_states: List[String]) extends WebSimMessage
  {
    lazy val toSite =  {
      val linkNames = site_links.map(l=>l._2.toString).toSet
      val states = site_states.map(s=>KappaModel.State(s)).toSet
      KappaModel.Site(site_name, states, linkNames)
    }
  }


  object ContactMap {
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[ContactMap] = boopickle.Default.generatePickler[ContactMap]
    lazy val empty = ContactMap(Nil)
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

    lazy val empty = RunModel("", None, None, None)
  }
  case class RunModel(code: String, nb_plot: Option[Int] = Some(250), max_events: Option[Int], max_time: Option[Double] = None) extends WebSimMessage with RunParameters

  trait RunParameters {
    def nb_plot: Option[Int]
    def max_events: Option[Int]
    def max_time: Option[Double]
  }

  object Observable {
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[Observable] = boopickle.Default.generatePickler[Observable]
  }
  case class Observable(observation_time: Double, observation_values: List[Double])  extends WebSimMessage

  object UnaryDistance {
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[UnaryDistance] = boopickle.Default.generatePickler[UnaryDistance]
  }
  //type distance = {rule_dist : string; time_dist : float; dist : int}
  case class UnaryDistance(rule_dist: String, time_dist: Double, dist: Int) extends WebSimMessage

  object KappaPlot {
    lazy val empty = KappaPlot(Nil, Nil)
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[KappaPlot] = boopickle.Default.generatePickler[KappaPlot]
  }
  case class KappaPlot(legend: List[String], time_series: List[Observable]) extends WebSimMessage {

    lazy val observables = time_series.reverse //in ASC position

    //println("LEGEND IS: "+ legend.mkString(" | "))
    //println("kappa plot: "+legend.toList)

    //println("==========TIMESERIES========\n"+time_series.map(o=>"TIME "+o.observation_time+ "VALUES: "+o.observation_values.mkString(" ")).mkString("\n"))
    lazy val timePoints: List[Double] = observables.foldLeft(List.empty[Double])((acc, o)=> o.observation_time::acc).reverse

  }

  object FluxMap {
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[FluxMap] = boopickle.Default.generatePickler[FluxMap]
  }

  case class FluxMap(
                      flux_begin_time: Double,
                      flux_end_time: Double,
                      flux_rules: List[String],
                      flux_hits: List[Int],
                      flux_fluxs: List[List[Double]],
                      flux_name: String) extends WebSimMessage

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

  /*

  type site_node = {
    node_quantity: float option;
    node_name: string;
    node_sites: site Ag_util.ocaml_array
  }

  type site_graph = site_node Ag_util.ocaml_array

  type snapshot = {
    snap_file : string;
    snap_event : int;
    agents : (int * site_graph) list;
    tokens : (float * string) list;
  }
  */


  case class Snapshot(snap_file: String, snap_event: Int, agents: List[(Int, List[WebSimNode])]/*, tokens: List[TokenState]*/) extends WebSimMessage
  {
    lazy val toKappaSnapshot = {
      val patterns = agents.map{ case (q, list) => (Pattern(list.zipWithIndex.map{ case (ag, i)=> ag.toAgent.copy(position = i)}) ,q) }.toMap
      KappaSnapshot(snap_file, snap_event, patterns)
    }
  }

  object FileLine {
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[FileLine] = boopickle.Default.generatePickler[FileLine]
  }
  case class FileLine(file_name: String, line: String) extends WebSimMessage

  object SimulationStatus {
    lazy val empty = SimulationStatus(0.0,
      None, 0, None, None, None, None, None, is_running = false, None , Nil, None, Nil, Nil, Nil, Nil
    )
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[SimulationStatus] = boopickle.Default.generatePickler[SimulationStatus]
  }

  /* OCAML class is:
    type simulator_state =
    { mutable is_running : bool
    ; mutable run_finalize : bool
    ; counter : Counter.t
    ; log_buffer : Buffer.t
    ; log_form : Format.formatter
    ; mutable plot : ApiTypes_j.plot
    ; mutable distances : ApiTypes_j.distances
    ; mutable snapshots : ApiTypes_j.snapshot list
    ; mutable flux_maps : ApiTypes_j.flux_map list
    ; mutable files : ApiTypes_j.file_line list
    ; mutable error_messages : ApiTypes_j.errors
    ; contact_map : Primitives.contact_map
    ; env : Environment.t
    ; mutable domain : Connected_component.Env.t
    ; mutable graph : Rule_interpreter.t
    ; mutable state : State_interpreter.t
    }
   */
  case class SimulationStatus(
                               time: Double,
                               time_percentage: Option[Double],
                               event: Int,
                               event_percentage: Option[Double],
                               tracked_events: Option[Int],
                               nb_plot: Option[Int],
                               max_time: Option[Double],
                               max_events: Option[Int],
                               is_running: Boolean,
                               code: Option[String],
                               log_messages: List[String],
                               plot: Option[KappaPlot],
                               snapshots: List[Snapshot],
                               distances: List[UnaryDistance],
                               flux_maps: List[FluxMap],
                               files: List[String]
                             )  extends WebSimMessage
  {
    def notFinished: Boolean = percentage < 100.0 && is_running//.getOrElse(true)

    lazy val percentage: Double = event_percentage.orElse(time_percentage).get //throw if neither events not time are set

    lazy val runParameters: RunModel = RunModel(code.getOrElse(""), nb_plot, max_events, max_time)

    lazy val max: Option[Double] = max_time.orElse(max_events.map(e=>e:Double))
  }
}