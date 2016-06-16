package org.denigma.kappa.notebook.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream._
import akka.stream.scaladsl._
import io.circe.generic.auto._
import org.denigma.kappa.messages.{WebSimError, RunModel, SimulationStatus, Version}

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util._
import akka.http.extensions._

trait PoolMessage
{
  def time: LocalDateTime
}

case class TimePoolMessage(time: LocalDateTime) extends PoolMessage

case class ModelPoolMessage(runParams: RunModel,
                            time: LocalDateTime,
                            token: Option[Int] = None,
                            results: List[SimulationStatus] = List.empty,
                            errors: List[String] = List.empty)
  extends PoolMessage

trait PooledWebSimFlows extends WebSimFlows {

  type TryResponse = (Try[HttpResponse], PoolMessage)

  implicit val system: ActorSystem

  implicit val mat: ActorMaterializer

  implicit protected def context: ExecutionContextExecutor = system.dispatcher

  protected def pool: Flow[(HttpRequest, PoolMessage), TryResponse, HostConnectionPool] //note: should be extended by lazy val

  val timePool: Flow[HttpRequest, TryResponse, NotUsed] = Flow[HttpRequest].map(req => req -> TimePoolMessage(LocalDateTime.now)).via(pool)

  val versionFlow = versionRequestFlow.via(timePool).via(unmarshalFlow[Version]).sync

  val running: Flow[Any, Array[Int], NotUsed] = runningRequestFlow.via(timePool).via(unmarshalFlow[Array[Int]].sync)

  def safeUnmarshalFlow[T, U](onfailure: (HttpResponse, Throwable) => Future[U])
                             (implicit um: Unmarshaller[HttpResponse, T]): Flow[TryResponse, Future[Either[T,U]], NotUsed] =
    Flow[TryResponse].map{
      case (Success(resp), message) =>
        val fut = Unmarshal(resp).to[T]
        fut.map[Either[T,U]](Left(_)).recoverWith{
          case exception => onfailure(resp, exception).map[Either[T,U]](Right(_))
        }
      case (Failure(exception), time) => Future.failed(exception)
    }

  val safeTokenUnmarshalFlow: Flow[TryResponse, Future[Either[Token, List[WebSimError]]], NotUsed] = safeUnmarshalFlow[Int, List[WebSimError]]{
    case (resp, time) =>
      Unmarshal(resp).to[List[WebSimError]].recoverWith{
        case th=>
          system.log.error("==\n WRONG UNMARSHAL FOR:\n "+resp+"==\n")
          Future.failed(th)
      }
  }

  def unmarshalFlow[T](implicit um: Unmarshaller[HttpResponse, T]): Flow[TryResponse, Future[T], NotUsed] = Flow[TryResponse].map{
    case (Success(resp), time) =>
      Unmarshal(resp).to[T].recoverWith{
        case th=>
          system.log.error("==\n WRONG UNMARSHAL FOR:\n "+resp+"==\n")
          Future.failed(th)
      }
    case (Failure(exception), time) =>
      Future.failed(exception)
  }

  def response2String(resp: HttpResponse, timeout: FiniteDuration = 300 millis): Future[String] = {
    resp.entity.toStrict(timeout).map { _.data }.map(_.utf8String)
  }

  val tokenFlow: Flow[RunModel, (Either[Token, List[WebSimError]], RunModel), NotUsed] = runModelRequestFlow.inputZipWith(timePool.via(safeTokenUnmarshalFlow.sync)){
    case (params, either)=> either -> params
  }

  val simulationStatusFlow: Flow[Token, (Token, SimulationStatus), NotUsed] =
    simulationStatusRequestFlow.inputZipWith(timePool.via(unmarshalFlow[SimulationStatus]).sync){
    case (token, result)=> token -> result
  }

  def simulationStream(updateInterval: FiniteDuration, parallelism: Int) =  Flow[Token].flatMapMerge(parallelism, {
    case token =>
      val source = Source.tick(0 millis, updateInterval, token)
      source.via( simulationStatusFlow )
        .upTo{
          case (t, sim) =>
            sim.percentage >= 100.0 || !sim.is_running//.getOrElse(false)
        }
  })

  lazy val syncSimulationStream: Flow[Token, (Token, SimulationStatus), NotUsed] = simulationStream(300 millis, 1)

  def simulationResultStream(updateInterval: FiniteDuration, parallelism: Int): Flow[RunModel,
    (Either[(Token, SimulationStatus), List[WebSimError]], RunModel), NotUsed] =
    tokenFlow.flatMapMerge(parallelism, {
      case (Left(token), model) =>
        val source = Source.tick(0 millis, updateInterval, token)
        source.via( simulationStatusFlow ).upTo{
          case (t, sim) => sim.percentage >= 100.0 || !sim.is_running
        }.map(v=>Left(v)->model)

      case (Right(errors), model) =>
        Source.single(Right(errors) -> model)
    }
    )

  lazy val syncSimulationResultStream: Flow[RunModel, (Either[(Token, SimulationStatus), List[WebSimError]], RunModel), NotUsed] = simulationResultStream(300 millis, 1)

  /*
  def test() = {
    simulationStatusRequestFlow.inputZipWith(timePool.via(unmarshalFlow[SimulationStatus]).sync){
      case (token, result)=> token -> result
    }
  }
  */

  val stopFlow: Flow[Token, (Token, SimulationStatus), NotUsed] = stopRequestFlow.inputZipWith(timePool.via(unmarshalFlow[SimulationStatus]).sync){
    case (token, result)=> token -> result
  }

}
