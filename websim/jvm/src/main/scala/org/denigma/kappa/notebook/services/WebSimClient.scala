package org.denigma.kappa.notebook.services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream._
import akka.stream.scaladsl._
import org.denigma.kappa.messages.ServerMessages.{LaunchModel, ServerConnection}
import org.denigma.kappa.messages.WebSimMessages._

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}

/**
  * Main class to connect to WebSim server
  * @param connectionParameters Connection parameters to connect with WebSim server
  * @param system actor system
  * @param mat materializer
  */
class WebSimClient(connectionParameters: ServerConnection)(implicit val system: ActorSystem, val mat: ActorMaterializer)
{

  type Token = Int //Kappa simmulation ID

  type ContactMapResult = scala.Either[ContactMap, List[WebSimError]]

  val flows = new WebSimClientFlows(connectionParameters.host, connectionParameters.port)

  protected lazy val ModelPool = Http().cachedHostConnectionPool[ModelPoolMessage](connectionParameters.host, connectionParameters.port)

  /**
    * @return version of WebSim API
    */
  def getVersion(): Future[Version] = Source.single(Unit).via(flows.versionFlow).runWith(Sink.head)

  /**
    * @param toParse Case class with sources to parse
    * @return Either ContactMap for valid model or SyntaxErrors
    */
  def parse(toParse: ParseCode): Future[ContactMapResult] = {
    val source = Source.single(toParse)
    source.via(flows.parseFlow).map(_._1) runWith Sink.head
  }

  def parse(code: String): Future[ContactMapResult]  = parse(ParseCode(code))

  /**
    * Parses the model and returns either ContactMap or a list of syntax errors
    * @param toParse case class with parse information
    * @param sink sink to put the data into
    * @tparam T Result type
    * @return
    */
  def parse[T](toParse: ParseCode, sink: Sink[ContactMapResult, T]): T = {
    val source = Source.single(toParse)
    source.via(flows.parseFlow).map(_._1) runWith sink
  }

  /**
    * Launches the model and returns Model ID + ContactMap + Initial parameters
    * @param model kappa model
    * @return
    */
  def launch(model: LaunchModel): Future[(flows.TokenContactResult, LaunchModel)] = {
    val source = Source.single(model) // give one model
    source.via(flows.runModelFlow) runWith Sink.head
  }

  /*
  Stops the simulations
   */
  def stop(token: Token): Future[SimulationStatus] = {
    val source: Source[Token, NotUsed] = Source.single(token)
    source.via(flows.simulationStatusFlow).map(_._2) runWith Sink.head
  }

  /**
    *  Runs the simulation and
    * @param model kappa model and running parameters
    * @return returns either Token+SimulationResults (with charts and other data) + Contact Map or syntax errors
    */
  def run(model: LaunchModel): Future[flows.Runnable[flows.SimulationContactResult]] =  {
    val source = Source.single(model)
    source.via(flows.syncSimulationResultStream).runWith(Sink.last)
  }


  /**
    * Runs the simulation and
    * @param model kappa model and running parameters
    * @param updateInterval how often to check the status update
    * @param parallelism requests in parallel
    * @return
    */
  def run(model: LaunchModel,
          updateInterval: FiniteDuration,
          parallelism: Int = 1): Future[flows.Runnable[flows.SimulationContactResult]] = {
    runStreamed(model, Sink.last, updateInterval, parallelism)
  }

  /**
    * @param model Kappa Model to run
    * @param sink Sink - where to stream the result
    * @param updateInterval how often to update running status of the simulation
    * @param parallelism
    * @tparam T Result type
    * @return
    */
  def runStreamed[T](model: LaunchModel,
                     sink: Sink[flows.Runnable[flows.SimulationContactResult], T],
                     updateInterval: FiniteDuration, parallelism: Int = 1): T = {
    val source = Source.single(model)
    val withFlow: Source[flows.Runnable[flows.SimulationContactResult], NotUsed] = source.via(flows.simulationResultStream(updateInterval, parallelism))
    withFlow.runWith(sink)
  }

  /**
    * Gets Simulation result by Simulation ID
    * @param token ID of simulation
    * @param updateInterval how often to update status
    * @param parallelism
    * @return SimulationResult
    */
  def resultByToken(token: Token,  updateInterval: FiniteDuration = 400 millis, parallelism: Int = 1) =
    Source.single(token)
      .via(flows.simulationStream(updateInterval, parallelism))
      .map(_._2).runWith(Sink.last)

  /**
    * @return Array with IDs of all simulations that are being run in the server
    */
  def getRunning(): Future[Array[Token]] = {
    val source = Source.single(Unit)
    source.via(flows.running) runWith Sink.head
  }

}
