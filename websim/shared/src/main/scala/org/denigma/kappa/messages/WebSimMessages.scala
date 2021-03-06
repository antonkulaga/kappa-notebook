package org.denigma.kappa.messages

import boopickle.CompositePickler
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{KappaSnapshot, Pattern, Site}

import scala.List
import scala.Predef.{Map, Set}
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

  /*
  [{"severity":"error","message":"invalid use of character &","range":{"file":"","from_position":{"chr":236,"line":9},"to_position":{"chr":236,"line":9}}}]),HttpProtocol(HTTP/1.1))==

   */


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

  case class WebSimError(severity: String, message: String, range: Option[WebSimRange] = None) extends WebSimMessage
  {
    lazy val fullMessage = s"[$severity] $message $rangeString"
    lazy val rangeString: String = range match {
      case Some(rangeValue) => s" :"+rangeValue.from_position.line+":" +
        rangeValue.from_position.chr + "-" +
        rangeValue.to_position.line + ":" +
        rangeValue.to_position.chr
      case None => ""
     }
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

    /**
      * Due to differences in link representation in WebSim and KappaModel we have to keep the correspondence
      * @param agentPosition
      * @param linkMap
      * @return
      */
    def toAgent(agentPosition: Int, linkMap: Map[((Int, Int), (Int, Int)), String]): KappaModel.Agent = {
      val sites = node_sites.zipWithIndex.map{ case (site, index) => site.toSite((agentPosition, index), linkMap) }.toSet
      KappaModel.Agent(node_name, sites, agentPosition)
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

  /**
    *
    * @param site_name name of site
    * @param site_links List of [(agent_index, site_index)]
    * @param site_states list of site states
    */
  case class WebSimSite(site_name: String, site_links: List[(Int, Int)], site_states: List[String]) extends WebSimMessage
  {
    def toSite(myPosition: (Int, Int), linkMap: Map[((Int, Int), (Int, Int)), String]): KappaModel.Site =  {
      val states = site_states.map(s => KappaModel.State(s)).toSet
      if(site_links.isEmpty) KappaModel.Site(site_name, states) else {
        val links = site_links.map{ to =>
          val pos = (myPosition, to)
          linkMap.getOrElse(pos, linkMap(pos.swap)) //WARNING: UNSAFE
        }.toSet
        KappaModel.Site(site_name, states, links)
      }
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

    lazy val empty = RunModel("", 0.1, None, None)
  }
  case class RunModel(code: String,
                      plot_period: Double = 0.1,
                      max_events: Option[Int],
                      max_time: Option[Double] = None,
                      seed: Option[Int] = Some(0),
                      runCount: Int = 1) extends WebSimMessage with RunParameters

  trait RunParameters {
    def plot_period: Double
    def max_events: Option[Int]
    def max_time: Option[Double]
    def runCount: Int
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

    private def nodes2Pattern(nodes: List[WebSimNode]): Pattern = {
      val indexed = nodes.zipWithIndex
      val links: List[((Int, Int), (Int, Int))] = for{
        (node, ni) <- indexed
        (site, si) <- node.node_sites.zipWithIndex
        (toAgent, toSite) <- site.site_links
      } yield ((ni, si), (toAgent, toSite))

      val (_, linkMap: Map[((Int, Int), (Int, Int)), String]) = links.foldLeft((1, Map.empty[((Int, Int), (Int, Int)), String])){
        case ((index, mp), key) =>
          if(mp.contains(key) || mp.contains(key.swap)) (index, mp) else (index + 1, mp.updated(key, index.toString))
      } //due to differences in link formats we have to do this crazy conversion
      val result = Pattern(indexed.map{ case (node, index) => node.toAgent(index, linkMap)})
      result
    }

    lazy val toKappaSnapshot = {
      val patterns = agents.map{ case (q, list) => (nodes2Pattern(list), q)}.toMap
      KappaSnapshot(snap_file, snap_event, patterns)
    }
    /*
        lazy val toDebugKappaSnapshot = {
          val patterns = agents.map{ case (q, list) =>
            pprint.pprintln("===ORIGIN===")
            pprint.pprintln(list)
            val pat = nodes2Pattern(list)
            pprint.pprintln("===RESULT===")
            pprint.pprintln(pat)
            (pat, q)
          }.toMap
          KappaSnapshot(snap_file, snap_event, patterns)
        }
      */
  }

  object FileLine {
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[FileLine] = boopickle.Default.generatePickler[FileLine]
  }

  case class FileLine(file_name: String, line: String) extends WebSimMessage

  object SimulationStatus {
    lazy val empty = SimulationStatus(0.0,
      None, 0, Some(0), None, None, None, is_running = false, Nil, None, Nil, None, Nil, Nil
    )
    import boopickle.DefaultBasic._
    implicit val classPickler: Pickler[SimulationStatus] = boopickle.Default.generatePickler[SimulationStatus]
  }


  /**
    * type state = { plot : plot nullable;
               distances : distances nullable;
               time : float;
               time_percentage : int nullable;
               event : int;
               event_percentage : int nullable;
               tracked_events : int nullable;
               log_messages : string list;
               snapshots : snapshot list;
               flux_maps : flux_map list;
               files : file_line list;
               is_running : bool
             }
    */
  case class SimulationStatus(
                               time: Double,
                               time_percentage: Option[Double],
                               event: Int,
                               event_percentage: Option[Double],
                               tracked_events: Option[Int],
                               //plot_period: Double,
                               max_time: Option[Double],
                               max_events: Option[Int],
                               is_running: Boolean,
                               log_messages: List[String],
                               plot: Option[KappaPlot],
                               snapshots: List[Snapshot],
                               distances: Option[List[UnaryDistance]],
                               flux_maps: List[FluxMap],
                               files: List[FileLine],
                               seed: Option[Int] = None
                             )  extends WebSimMessage
  {

    lazy val code = files.foldLeft(""){ case (acc, fl) => acc + "\n" + fl.line}

    lazy val stillRunning: Boolean = percentage < 100.0 && is_running//.getOrElse(true)

    lazy val percentage: Double = {
      val per = event_percentage.orElse(time_percentage).getOrElse(0.0)
      per
    } //throw if neither events not time are set

    lazy val stopped = !is_running && percentage < 100.0

    //lazy val runParameters: RunModel = RunModel(code, plot_period, max_events, max_time)

    lazy val max: Option[Double] = max_time.orElse(max_events.map(e=>e:Double))
  }
}