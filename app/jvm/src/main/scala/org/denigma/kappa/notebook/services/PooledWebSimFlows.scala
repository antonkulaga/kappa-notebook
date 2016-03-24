package org.denigma.kappa.notebook.services
import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.{ActorMaterializer, Materializer}
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

trait PoolMessage
{
  def time: LocalDateTime
}

case class TimePoolMessage(time: LocalDateTime) extends PoolMessage
case class TokenPoolMessage(token: Int, time: LocalDateTime) extends PoolMessage


trait PooledWebSimFlows extends WebSimFlows  {

  type TryRequest = (Try[HttpResponse], PoolMessage)

  implicit val system: ActorSystem

  implicit val mat: ActorMaterializer

  implicit protected def context: ExecutionContextExecutor = system.dispatcher

  protected def pool: Flow[(HttpRequest, PoolMessage), (Try[HttpResponse], PoolMessage), HostConnectionPool]

  val timePool = Flow[HttpRequest].map(req => req -> TimePoolMessage(LocalDateTime.now)).via(pool)

  val tokenPool = Flow[(Token, HttpRequest)].map{ case (token, req) => req -> TokenPoolMessage(token, LocalDateTime.now)}.via(pool)

  def unmarshalPoolFlow[T](implicit um: Unmarshaller[HttpResponse, T]): Flow[TryRequest, Future[T], NotUsed] = Flow[TryRequest].map{
    case (Success(req), time) => Unmarshal(req).to[T]
    case (Failure(exception), time) => Future.failed(exception)
  }


  protected val simulationStatusFutureFlow: Flow[Int, Future[(TokenPoolMessage, WebSim.SimulationStatus)], NotUsed] = simulationStatusRequestFlow.via(tokenPool).map{
    case (Success(res), mess: TokenPoolMessage) =>
      //pprint.pprintln("ENTITY "+res.entity)
      Unmarshal(res).to[WebSim.SimulationStatus].map(st=>mess -> st)
    case (Failure(th), mess: TokenPoolMessage) => Future.failed(th)
    case (other, mess) => Future.failed(new Exception(s"Message $mess should be Tocken"))
  }

  def makeModelFinalResultFlow(parallelism: Int, streamInterval: FiniteDuration) = makeModelResultsFlow(parallelism, streamInterval).dropWhile{
    case (token, sim) => sim.notFinished
  }

  def makeModelResultsFlow(parallelism: Int, streamInterval: FiniteDuration): Flow[RunModel, (TokenPoolMessage, SimulationStatus), NotUsed] = runModelRequestFlow.via(timePool)
    .via(unmarshalPoolFlow[Token]).mapAsync(parallelism)(identity(_)).via(makeTokenResultsFlow(parallelism, streamInterval))

  def makeTokenResultsFlow(parallelism: Int, streamInterval: FiniteDuration): Flow[Token, (TokenPoolMessage, SimulationStatus), NotUsed] =  Flow[Token].flatMapMerge(parallelism, {
      case token =>
        val source = Source.tick(0 millis, streamInterval, token)
        source.via( simulationStatusFutureFlow.mapAsync(1)(identity(_)))
          .upTo{
            case (t, sim) => sim.percentage >= 100.0 || !sim.is_running//.getOrElse(false)
          }
  })
}
