package org.denigma.kappa.notebook.services

import java.time.LocalDateTime

import akka.{Done, NotUsed}
import akka.actor.{Cancellable, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.stage._
import akka.stream._
import akka.stream.scaladsl._
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.denigma.kappa.WebSim
import org.denigma.kappa.WebSim.SimulationStatus
import io.circe._
import io.circe.generic.auto._
import org.denigma.kappa.extensions._
import scala.collection.immutable.Seq

//NOTE: sole IDEs like Intellij think that import is unused while it is used
import io.circe.parser._
import io.circe.syntax._
import scala.concurrent.duration._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util._

/**
  * Created by antonkulaga on 04/03/16.
  */
class WebSimClient(host: String = "localhost", port: Int = 8080)(implicit val system: ActorSystem, val mat: ActorMaterializer) extends WebSimFlows {

  implicit protected def context: ExecutionContextExecutor = system.dispatcher

  protected val pool: Flow[HttpRequest, (Try[HttpResponse], LocalDateTime), NotUsed] = {
    val p = Http().cachedHostConnectionPool[LocalDateTime](host, port)
    Flow[HttpRequest].map(req=> (req, LocalDateTime.now())).via(p)
  }

  protected val resultsFlow: Flow[Int, SimulationStatus, NotUsed] = resultsRequestFlow.via(pool).map{
    case (Success(res), time) => Unmarshal(res).to[SimulationStatus]
    case (Failure(th), time) => Future.failed(th)
  }.mapAsync(1)(identity)

  /**
    * Versin of WebSim API
    */
  val base = "/v1"

  protected def exec(source:  Source[(Try[HttpResponse], LocalDateTime), NotUsed]): Future[HttpResponse] = {
    source
      .runWith(Sink.head).flatMap {
      case (Success(r: HttpResponse), _) ⇒ Future.successful(r)
      case (Failure(f), _) ⇒ Future.failed(f)
    }
  }

  def getVersion()= {
    val source = Source.single(Unit).via(versionRequestFlow)
    exec(source.via(pool)) flatMap {
      case req =>  Unmarshal(req).to[WebSim.VersionInfo]
    }
  }

  def run(model: WebSim.RunModel): Future[Int] =  {
    val source = Source.single(model).via(runModelRequestFlow).via(pool)
    exec(source) flatMap(req => Unmarshal(req).to[Int])
  }

  def getResult(token: Int): Future[SimulationStatus] = Source.single(token).via(resultsFlow) runWith(Sink.last)

  def runWithStreaming[Mat](model: WebSim.RunModel, sink: Sink[SimulationStatus, Mat], interval: FiniteDuration = 500 millis): Future[Mat] = run(model).map{
    case token => streamResults(token, sink, interval)
  }

  def runWithStreamingFlatten[Mat](model: WebSim.RunModel, sink: Sink[SimulationStatus, Future[Mat]], interval: FiniteDuration = 500 millis): Future[Mat] = run(model).flatMap{
    case token => streamResults(token, sink, interval)
  }

  def runWithResult(model: WebSim.RunModel, interval: FiniteDuration = 500 millis): Future[SimulationStatus] = {
   runWithStreamingFlatten(model, Sink.last, interval)
  }

  def streamResults[Mat](token: Int, sink: Sink[SimulationStatus, Mat], interval: FiniteDuration = 500 millis): Mat = {
    val tick: Source[Int, Cancellable] = Source.tick(0 millis, interval, token)
    val results: Source[WebSim.SimulationStatus, Cancellable] = tick.via(resultsFlow)
    val stream: Source[WebSim.SimulationStatus, Cancellable] = results.upTo{
        case sim =>
          //println("----------------------------")
          //println(s"percentage = ${sim.percentage}")
          //println(sim)
          sim.percentage >= 100.0 || !sim.is_running.getOrElse(false)
      }
    stream.runWith[Mat](sink)
  }

  def getRunning(): Future[Array[Int]] = {
    val source: Source[HttpRequest, NotUsed] = Source.single(Unit).via(runningRequestFlow)
    exec(source.via(pool)) flatMap {
      case req => Unmarshal(req).to[Array[Int]]
    }
  }

}
