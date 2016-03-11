package org.denigma.kappa.WebSim

import boopickle.Default._
import scala.collection.immutable._

  class WebSimPicklers {


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

  }


  object Defaults
  {
    lazy val runModel: RunModel = RunModel(code = "", max_events = Some(10000), max_time = None)
    lazy val simulationStatus: SimulationStatus = SimulationStatus(None, None, None, None, None, None, None, None, None, None, None, Array.empty[FluxMap])
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
                               is_running: Option[Boolean],
                               code: Option[String],
                               logMessages: Option[String],
                               plot: Option[KappaPlot],
                               flux_maps: Array[FluxMap]
                             )  extends WebSimMessage
  {
    def percentage: Double = event_percentage.orElse(time_percentage).get //showd throw if neither events not time are set
  }

  case class Observable(time: Double, values: Array[Double])  extends WebSimMessage

  case class KappaPlot(legend: Array[String], observables: Array[Observable]) extends WebSimMessage

  case class FluxData(flux_name: String) extends WebSimMessage

  case class FluxMap(flux_data: FluxData, flux_end: Double) extends WebSimMessage

  case class Code(code: String) extends WebSimMessage
  {
    def isEmpty = code == ""
  }

  case class Load(filename: String) extends WebSimMessage