package org.denigma.kappa.notebook.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, _}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.denigma.kappa.WebSim
import org.denigma.kappa.WebSim.SimulationStatus
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._


import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

/**
  * Basic idea of the API is that we create a flow for each type of request to websim API, then those flows are applied to http pool
  */
trait WebSimFlows extends CirceSupport {

  /*
  implicit def system: ActorSystem

  implicit def mat: ActorMaterializer
  */

  implicit def context: ExecutionContextExecutor


  def base: String

  protected val versionRequestFlow: Flow[Any, HttpRequest, NotUsed] = Flow[Any].map(any => HttpRequest(uri = s"$base/version"))

  protected val runningRequestFlow: Flow[Any, HttpRequest, NotUsed] = Flow[Any].map(any => HttpRequest(uri = s"$base/process", method = HttpMethods.GET))

  /**
    * Flow where you provide Models to get httpRequest in return
    */
  protected val runModelRequestFlow = Flow[WebSim.RunModel].map{
    case model =>
      val json = model.asJson.noSpaces
      val data = HttpEntity(ContentTypes.`application/json`, json)
      HttpRequest.apply(uri = s"$base/process", method = HttpMethods.POST, entity =  data)
  }


  protected val resultsRequestFlow = Flow[Int].map{
    case token => HttpRequest.apply(uri = s"$base/process/$token", method = HttpMethods.GET)
  }

}

/**
  * Created by antonkulaga on 04/03/16.
  */
class WebSimClient(host: String = "localhost", port: Int = 8080)(implicit val system: ActorSystem, val mat: ActorMaterializer) extends WebSimFlows {

  implicit def context: ExecutionContextExecutor = system.dispatcher


  /**
    * Connection pool to WebSim server
    * Uses LocalDateTime to mark the evets
    */
  protected val pool: Flow[HttpRequest, (Try[HttpResponse], LocalDateTime), NotUsed] = {
    val p = Http().cachedHostConnectionPool[LocalDateTime](host, port)
    Flow[HttpRequest].map(req=> (req, LocalDateTime.now())).via(p)
  }

  /**
    * Versin of WebSim API
    */
  val base = "/v1"

  def exec(source:  Source[(Try[HttpResponse], LocalDateTime), NotUsed]): Future[HttpResponse] = {
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

  def getResult(token: Int): Future[SimulationStatus] = {
    val source= Source.single(token).via(resultsRequestFlow).via(pool)
    exec(source) flatMap(req => Unmarshal(req).to[WebSim.SimulationStatus])
  }


  def getRunning(): Future[Array[Int]] = {
    val source: Source[HttpRequest, NotUsed] = Source.single(Unit).via(runningRequestFlow)
    exec(source.via(pool)) flatMap {
      case req => Unmarshal(req).to[Array[Int]]
    }
  }
}
