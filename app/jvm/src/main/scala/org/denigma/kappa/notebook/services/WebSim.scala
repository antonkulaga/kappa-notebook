package org.denigma.kappa.notebook.services

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.Source
import akka.stream._
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.denigma.kappa.messages.KappaMessages.RunParameters

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Random, Try}
import scala.concurrent._
import scala.concurrent.duration._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import akka.http.scaladsl.unmarshalling.Unmarshal


object WebSim {

  case class RunModel(code: String, nb_plot: Int = 1000, max_events: Option[Int], max_time: Option[Double] = None)

  case class VersionInfo( build: String, version: String )

  case class Observable(time: Double, values: Array[Double])

  case class KappaPlot(observables: Array[Observable])

  case class FluxData(flux_name: String)

  case class FluxMap(flux_data: FluxData, flux_end: Double)

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
                           )
  {
    def percentage = event_percentage.orElse(time_percentage).get //showd throw if neither events not time are set
  }


  /*
  *
  *  Status {
      plot:
        undefined
      time_percentage:
        number
      event:
        integer
      event_percentage:
        integer
      tracked_events:
        integer
      log_messages:
        string
      unary_distances:
      []
      snapshots:
      []
      flux_maps:
      []
      files:
        string
      is_running:
        boolean
      code:
        string
      nb_plot:
        integer
      max_time:
        number
      max_events:
        integer
    }
  * */

}

/**
  * Created by antonkulaga on 04/03/16.
  */
class WebSim(host: String = "localhost", port: Int = 8080)(implicit val system: ActorSystem, val mat: ActorMaterializer) extends CirceSupport {

  implicit def context = system.dispatcher

  protected val pool = Http().cachedHostConnectionPool[Int](host, port)

  protected val base = "/v1"

  protected val version = Source.single(HttpRequest(uri = s"$base/version") -> 1).via(pool)

  protected val running = {
    Source.single(HttpRequest(uri = s"$base/process", method = HttpMethods.GET) -> 1).via(pool)
  }


  protected def run(model: WebSim.RunModel) = {
    val json = model.asJson.noSpaces
    val data = HttpEntity(ContentTypes.`application/json`, json)
    Source.single(HttpRequest.apply(uri = s"$base/process", method = HttpMethods.POST, entity =  data) -> 1).via(pool)
  }

  protected def getResult(token: Int) = {
    Source.single(HttpRequest.apply(uri = s"$base/process/$token", method = HttpMethods.GET) -> 1).via(pool)
  }



  def exec(source:  Source[(Try[HttpResponse], Int), NotUsed]): Future[HttpResponse] = {
    source
      .runWith(Sink.head).flatMap {
      case (Success(r: HttpResponse), _) ⇒ Future.successful(r)
      case (Failure(f), _) ⇒ Future.failed(f)
    }
  }

  def getVersion()= exec(version) flatMap {
    case req =>  Unmarshal(req).to[WebSim.VersionInfo]
  }

  def runSimulation(model: WebSim.RunModel) = {
    //require(points <= events, s"number of data points should not be larger than the number of events")
    exec(run(model)) flatMap {
      case req if req.status == StatusCodes.OK =>
        Unmarshal(req).to[Int]

      case req if req.status == StatusCodes.BadRequest =>
        val message = "BAD REQUEST = "+req.entity
        println(message)
        Future.failed(new Exception(message))

      case req =>
        val code = req.status.value
        val reason = req.status.reason()
        val message = s"CODE $code with $reason = "+ req.entity.withContentType(ContentTypes.`text/plain(UTF-8)`)
        println(message)
        Future.failed(new Exception(message))
    }
  }

  def getSimulation(token: Int) = {
    exec(getResult(token)) flatMap {
      case req => Unmarshal(req).to[WebSim.SimulationStatus]
    }

  }

  def getRunning(): Future[Array[Int]] = {
    exec(running) flatMap {
      case req => Unmarshal(req).to[Array[Int]]
    }
  }
}
