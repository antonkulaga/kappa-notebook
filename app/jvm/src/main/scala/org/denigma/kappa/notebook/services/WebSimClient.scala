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
import akka.stream.impl.fusing.GraphStages
import akka.stream.scaladsl._
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.denigma.kappa.extensions._

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util._
import org.denigma.kappa.extensions._
import org.denigma.kappa.messages.{RunModel, SimulationStatus}

import scala.Either

class WebSimClientFlows(host: String = "localhost", port: Int = 8080)(implicit val system: ActorSystem, val mat: ActorMaterializer) extends PooledWebSimFlows
{
  val base = "/v1"

  val defaultParallelism = 1

  val defaultUpdateInterval = 500 millis

  protected lazy val pool: Flow[(HttpRequest, PoolMessage), (Try[HttpResponse], PoolMessage), HostConnectionPool] = Http().cachedHostConnectionPool[PoolMessage](host, port)

}

class WebSimClient(host: String = "localhost", port: Int = 8080)(implicit val system: ActorSystem, val mat: ActorMaterializer)
{
  //def stop(token: Int) =

  type Token = Int

  val flows = new WebSimClientFlows(host, port)

  protected lazy val ModelPool = Http().cachedHostConnectionPool[ModelPoolMessage](host, port)

  def getVersion() = Source.single(Unit).via(flows.versionFlow).runWith(Sink.head)


  def launch(model: RunModel): Future[Either[Token, Array[String]]] = {
    val source = Source.single(model) //give one model
    source.via(flows.tokenFlow).map(_._1) runWith Sink.head
  }

  def stop(token: Token): Future[SimulationStatus] = {
    val source: Source[Token, NotUsed] = Source.single(token)
    source.via(flows.simulationStatusFlow).map(_._2) runWith Sink.head
  }

  def run(model: RunModel): Future[(Either[(flows.Token, SimulationStatus), Array[String]], RunModel)] =  {
    val source = Source.single(model)
    source.via(flows.syncSimulationResultStream).runWith(Sink.last)
  }

  def run(model: RunModel, updateInterval: FiniteDuration, parallelism: Int = 1) =  {
    runStreamed(model, Sink.last, updateInterval, parallelism)
  }

  def runStreamed[T](model: RunModel, sink: Sink[(Either[(Int, SimulationStatus), Array[String]], RunModel), T], updateInterval: FiniteDuration, parallelism: Int = 1) =  {
    val source = Source.single(model)
    val withFlow = source.via(flows.simulationResultStream(updateInterval, parallelism))
    withFlow.runWith(sink)
      //.runWith(sink)
  }


  //def resultByToken(token: Int): Future[SimulationStatus] =  resultByToken(token, defaultUpdateInterval, defaultParallelism)

  def resultByToken(token: Token,  updateInterval: FiniteDuration = 400 millis, parallelism: Int = 1) =
    Source.single(token).via(flows.simulationStream(updateInterval, parallelism)).map(_._2).runWith(Sink.last) //Source.single(token).via(makeTokenResultsFlow(parallelism, updateInterval)) map(_._2) runWith Sink.last

  def getRunning(): Future[Array[Int]] = {
    val source = Source.single(Unit)
    source.via(flows.running) runWith Sink.head
  }

}
