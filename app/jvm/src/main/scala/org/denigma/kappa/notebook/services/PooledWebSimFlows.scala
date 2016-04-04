package org.denigma.kappa.notebook.services
import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream._
import akka.stream.scaladsl._
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util._
import org.denigma.kappa.WebSim
import org.denigma.kappa.WebSim.{RunModel, SimulationStatus, VersionInfo}

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.FiniteDuration
import org.denigma.kappa.extensions._

import scala.Either

trait PoolMessage
{
  def time: LocalDateTime
}

case class TimePoolMessage(time: LocalDateTime) extends PoolMessage
//case class TokenPoolMessage(token: Int, time: LocalDateTime) extends PoolMessage
case class ModelPoolMessage(runParams: RunModel, time: LocalDateTime, token: Option[Int] = None, results: List[SimulationStatus] = List.empty, errors: List[String] = List.empty) extends PoolMessage

trait PooledWebSimFlows extends WebSimFlows {

  type TryResponse = (Try[HttpResponse], PoolMessage)

  implicit val system: ActorSystem

  implicit val mat: ActorMaterializer

  implicit protected def context: ExecutionContextExecutor = system.dispatcher

  protected def pool: Flow[(HttpRequest, PoolMessage), TryResponse, HostConnectionPool] //note: should be extended by lazy val

  val timePool: Flow[HttpRequest, TryResponse, NotUsed] = Flow[HttpRequest].map(req => req -> TimePoolMessage(LocalDateTime.now)).via(pool)

  val versionFlow = versionRequestFlow.via(timePool).via(unmarshalFlow[VersionInfo]).sync


  val running: Flow[Any, Array[Int], NotUsed] = runningRequestFlow.via(timePool).via(unmarshalFlow[Array[Int]].sync)

  def safeUnmarshalFlow[T, U](onfailure: (HttpResponse, Throwable) => Future[U])(implicit um: Unmarshaller[HttpResponse, T]): Flow[TryResponse, Future[Either[T,U]], NotUsed] =
    Flow[TryResponse].map{
      case (Success(resp), message) =>
        val fut = Unmarshal(resp).to[T]
        fut.map[Either[T,U]](Left(_)).recoverWith{
          case exception => onfailure(resp, exception).map[Either[T,U]](Right(_))
        }
      case (Failure(exception), time) => Future.failed(exception)
    }

  val safeTokenUnmarshalFlow: Flow[TryResponse, Future[Either[Token, Array[String]]], NotUsed] = safeUnmarshalFlow[Int, Array[String]]{
    case (resp, time) =>
      Unmarshal(resp).to[Array[String]].recoverWith{
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

  val tokenFlow: Flow[RunModel, (Either[Token, Array[String]], RunModel), NotUsed] = runModelRequestFlow.inputZipWith(timePool.via(safeTokenUnmarshalFlow.sync)){
    case (params, either)=> either -> params
  }

  val simulationStatusFlow: Flow[Token, (Token, SimulationStatus), NotUsed] = simulationStatusRequestFlow.inputZipWith(timePool.via(unmarshalFlow[SimulationStatus]).sync){
    case (token, result)=> token -> result
  }
  /*
  def simulationResultStrem(parallelism: Int, streamInterval: FiniteDuration) = tokenFlow.flatMapMerge(parallelism, {
    case (Left(), model) =>
      val source = Source.tick(0 millis, streamInterval, token)
      source.via( simulationStatusFlow )
        .upTo{
          case (t, sim) =>
            sim.percentage >= 100.0 || !sim.is_running//.getOrElse(false)
        }
    case (Right(error), model) =>
      val source = Source.tick(0 millis, streamInterval, token)
      source.via( simulationStatusFlow )
        .upTo{
          case (t, sim) =>
            sim.percentage >= 100.0 || !sim.is_running//.getOrElse(false)
        }
  })
  */
  def simulationStream(parallelism: Int, streamInterval: FiniteDuration) =  Flow[Token].flatMapMerge(parallelism, {
    case token =>
      val source = Source.tick(0 millis, streamInterval, token)
      source.via( simulationStatusFlow )
        .upTo{
          case (t, sim) =>
            sim.percentage >= 100.0 || !sim.is_running//.getOrElse(false)
        }
  })

  lazy val syncSimulationStream: Flow[Token, (Token, SimulationStatus), NotUsed] = simulationStream(1, 300 millis)

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




  /*
  def makeTokenResultsFlow(parallelism: Int, streamInterval: FiniteDuration)=  Flow[(Token, RunModel)].flatMapMerge(parallelism, {
    case (token, params) =>
      val source = Source.tick(0 millis, streamInterval, token)
      source.via( simulationStatusFutureFlow.mapAsync(1)(identity(_)))
        .upTo{
          case (t, sim) =>
            sim.percentage >= 100.0 || !sim.is_running//.getOrElse(false)
        }
  })
  */
  /*
  protected val simulationStatusFutureFlow = simulationStatusRequestFlow.via(tokenPool).map{
    case (Success(res), mess: TokenPoolMessage) =>
      //pprint.pprintln("ENTITY "+res.entity)
      Unmarshal(res).to[WebSim.SimulationStatus].recoverWith{
        case th=>
          system.log.error("==\n WRONG UNMARSHAL FOR:\n "+res+"==\n")
          Future.failed(th)
      }.map(st=>mess -> st)
    case (Failure(th), mess: TokenPoolMessage) => Future.failed(th)
    case (other, mess) => Future.failed(new Exception(s"Message $mess should be Tocken"))
  }
  */
/*
  protected val simulationStatusFutureFlow: Flow[Int, Future[(TokenPoolMessage, WebSim.SimulationStatus)], NotUsed] = simulationStatusRequestFlow.via(tokenPool).map{
    case (Success(res), mess: TokenPoolMessage) =>
      //pprint.pprintln("ENTITY "+res.entity)
      Unmarshal(res).to[WebSim.SimulationStatus].recoverWith{
        case th=>
          system.log.error("==\n WRONG UNMARSHAL FOR:\n "+res+"==\n")
          Future.failed(th)
      }.map(st=>mess -> st)
    case (Failure(th), mess: TokenPoolMessage) => Future.failed(th)
    case (other, mess) => Future.failed(new Exception(s"Message $mess should be Tocken"))
  }

  def makeModelFinalResultFlow(parallelism: Int, streamInterval: FiniteDuration) = makeModelResultsFlow(parallelism, streamInterval).dropWhile{
    case (token, sim) => sim.notFinished
  }

  def makeModelResultsFlow(parallelism: Int, streamInterval: FiniteDuration): Flow[RunModel, (TokenPoolMessage, SimulationStatus), NotUsed] = runModelRequestFlow.via(timePool)
    .via(unmarshalFlow[Token]).mapAsync(parallelism)(identity(_)).via(makeTokenResultsFlow(parallelism, streamInterval))

  /*
  def makeTokenResultsFlow(parallelism: Int, streamInterval: FiniteDuration): Flow[Token, (TokenPoolMessage, SimulationStatus), NotUsed] =  Flow[Token].flatMapMerge(parallelism, {
      case token =>
        val source = Source.tick(0 millis, streamInterval, token)
        source.via( simulationStatusFutureFlow.mapAsync(1)(identity(_)))
          .upTo{
            case (t, sim) =>
              sim.percentage >= 100.0 || !sim.is_running//.getOrElse(false)
          }
  })
  */

 def makeTokenResultsFlow(parallelism: Int, streamInterval: FiniteDuration): Flow[Token, (TokenPoolMessage, SimulationStatus), NotUsed] =  Flow[Token].flatMapMerge(parallelism, {
    case token =>
      //val source = Source.tick(0 millis, streamInterval, token)
      val source = Source.repeat(token).delay(streamInterval)
      source.via( simulationStatusFutureFlow.mapAsync(1)(identity(_)))
        .upTo{
          case (t, sim) =>
            sim.percentage >= 100.0 || !sim.is_running
        }
  })

  */

}
