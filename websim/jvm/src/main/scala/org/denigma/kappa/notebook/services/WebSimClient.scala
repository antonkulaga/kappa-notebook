package org.denigma.kappa.notebook.services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import org.denigma.kappa.messages._

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util._
import akka.http.extensions._
import WebSimMessages._
import org.denigma.kappa.messages.ServerMessages.{LaunchModel, ServerConnection}
import pprint.PPrint.PPrint

class WebSimClientFlows(host: String = "localhost", port: Int = 8080)
                       (implicit val system: ActorSystem, val mat: ActorMaterializer)
  extends PooledWebSimFlows
{

  override def debug[T: PPrint](value: T) = {
    //println("let us debug!")
    implicit val config = pprint.Config(width = Int.MaxValue)
    val str = pprint.tokenize(value).reduce(_+_)
    system.log.debug(str)
  }

  val base = "/v1"

  val defaultParallelism = 1

  val defaultUpdateInterval = 500 millis

  protected lazy val pool: Flow[(HttpRequest, PoolMessage), (Try[HttpResponse], PoolMessage), HostConnectionPool] =
    Http().cachedHostConnectionPool[PoolMessage](host, port)

}

class WebSimClient(connectionParameters: ServerConnection)(implicit val system: ActorSystem, val mat: ActorMaterializer)
{
  //def stop(token: Int) =

  type Token = Int

  type ContactMapResult = scala.Either[ContactMap, List[WebSimError]]

  val flows = new WebSimClientFlows(connectionParameters.host, connectionParameters.port)

  protected lazy val ModelPool = Http().cachedHostConnectionPool[ModelPoolMessage](connectionParameters.host, connectionParameters.port)

  def getVersion(): Future[Version] = Source.single(Unit).via(flows.versionFlow).runWith(Sink.head)

  def parse[T](toParse: ParseCode, sink: Sink[ContactMapResult, T]): T = {
    val source = Source.single(toParse)
    source.via(flows.parseFlow).map(_._1) runWith sink
  }

  def parse(toParse: ParseCode): Future[ContactMapResult] = {
    val source = Source.single(toParse)
    source.via(flows.parseFlow).map(_._1) runWith Sink.head
  }
  def parse(code: String): Future[ContactMapResult]  = parse(ParseCode(code))

  def launch(model: LaunchModel): Future[(flows.TokenContactResult, LaunchModel)] = {
    val source = Source.single(model) // give one model
    source.via(flows.runModelFlow) runWith Sink.head
  }

  def stop(token: Token): Future[SimulationStatus] = {
    val source: Source[Token, NotUsed] = Source.single(token)
    source.via(flows.simulationStatusFlow).map(_._2) runWith Sink.head
  }

  def run(model: LaunchModel): Future[flows.Runnable[flows.SimulationContactResult]] =  {
    val source = Source.single(model)
    source.via(flows.syncSimulationResultStream).runWith(Sink.last)
  }

  def run(model: LaunchModel,
          updateInterval: FiniteDuration,
          parallelism: Int = 1): Future[flows.Runnable[flows.SimulationContactResult]] = {
    runStreamed(model, Sink.last, updateInterval, parallelism)
  }

  def runStreamed[T](model: LaunchModel,
                     sink: Sink[flows.Runnable[flows.SimulationContactResult], T],
                     updateInterval: FiniteDuration, parallelism: Int = 1): T = {
    val source = Source.single(model)
    val withFlow: Source[flows.Runnable[flows.SimulationContactResult], NotUsed] = source.via(flows.simulationResultStream(updateInterval, parallelism))
    withFlow.runWith(sink)
  }

  def resultByToken(token: Token,  updateInterval: FiniteDuration = 400 millis, parallelism: Int = 1) =
    Source.single(token).via(flows.simulationStream(updateInterval, parallelism)).map(_._2).runWith(Sink.last) //Source.single(token).via(makeTokenResultsFlow(parallelism, updateInterval)) map(_._2) runWith Sink.last

  def getRunning(): Future[Array[Int]] = {
    val source = Source.single(Unit)
    source.via(flows.running) runWith Sink.head
  }

}
