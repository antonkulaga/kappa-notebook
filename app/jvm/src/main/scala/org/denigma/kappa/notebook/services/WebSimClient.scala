package org.denigma.kappa.notebook.services

import java.time.LocalDateTime

import akka.{Done, NotUsed}
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.stage._
import akka.stream._
import akka.stream.scaladsl._
import org.denigma.kappa.WebSim
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.denigma.kappa.WebSim.{RunModel, SimulationStatus, VersionInfo}
import org.denigma.kappa.extensions._

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util._


class WebSimClient(host: String = "localhost", port: Int = 8080)(implicit val system: ActorSystem, val mat: ActorMaterializer) extends PooledWebSimFlows
{

  val defaultParallelism = 1
  val defaultUpdateInterval = 500 millis

  protected lazy val pool: Flow[(HttpRequest, PoolMessage), (Try[HttpResponse], PoolMessage), HostConnectionPool] = Http().cachedHostConnectionPool[PoolMessage](host, port)


  /**
    * Versin of WebSim API
    */
  val base = "/v1"


  def getVersion()= {
    Source.single(Unit).via(versionRequestFlow).via(timePool).via(unmarshalPoolFlow[VersionInfo]).mapAsync(1)(identity(_)).runWith(Sink.head)
  }

  def launch(model: WebSim.RunModel): Future[Token] = {
    Source.single(model).via(runModelRequestFlow).via(timePool).via(unmarshalPoolFlow[Int]).mapAsync(1)(identity(_)).runWith(Sink.head)
  }

  def run(model: WebSim.RunModel): Future[SimulationStatus] =  {
    Source.single(model).via(defaultRunModelFlow).map(_._2).runWith(Sink.last)
  }

  def run(model: WebSim.RunModel, updateInterval: FiniteDuration, parallelism: Int = 1): Future[SimulationStatus] =  {
    Source.single(model).via(makeModelResultsFlow(parallelism, updateInterval)).map(_._2).runWith(Sink.last)
  }

  lazy val defaultRunModelFlow: Flow[RunModel, (TokenPoolMessage, SimulationStatus), NotUsed] = makeModelResultsFlow(defaultParallelism, defaultUpdateInterval)

  /**
    * flow that returns only final results
    */
  lazy val defaultRunModelFinalResultFlow: Flow[RunModel, (TokenPoolMessage, SimulationStatus), NotUsed] = makeModelFinalResultFlow(defaultParallelism, defaultUpdateInterval)

  def resultByToken(token: Int): Future[SimulationStatus] =  resultByToken(token, defaultUpdateInterval, defaultParallelism)

  def resultByToken(token: Token,  updateInterval: FiniteDuration, parallelism: Int): Future[SimulationStatus] = Source.single(token).via(makeTokenResultsFlow(parallelism, updateInterval)) map(_._2) runWith Sink.last

  def getRunning(): Future[Array[Int]] = {
    Source.single(Unit).via(runningRequestFlow).via(timePool).via(unmarshalPoolFlow[Array[Int]]).mapAsync(1)(identity(_)) runWith Sink.head
  }

}
