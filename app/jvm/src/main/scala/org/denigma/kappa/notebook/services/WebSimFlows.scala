package org.denigma.kappa.notebook.services

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl._
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.denigma.kappa.WebSim
import org.denigma.kappa.WebSim.RunModel
import io.circe.syntax._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Basic idea of the API is that we create a flow for each type of request to websim API, then those flows are applied to http pool
  */
trait WebSimFlows extends CirceSupport{

  type Token = Int


//  implicit def context: ExecutionContextExecutor

  def base: String


  protected val versionRequestFlow: Flow[Any, HttpRequest, NotUsed] = Flow[Any].map(any => HttpRequest(uri = s"$base/version"))

  protected val runningRequestFlow: Flow[Any, HttpRequest, NotUsed] = Flow[Any].map(any => HttpRequest(uri = s"$base/process", method = HttpMethods.GET))
  /**
    * Flow where you provide Models to get httpRequest in return
    */
  protected val runModelRequestFlow: Flow[RunModel, HttpRequest, NotUsed] = Flow[WebSim.RunModel].map{
    case model =>
      val json = model.asJson.noSpaces
      val data = HttpEntity(ContentTypes.`application/json`, json)
      HttpRequest(uri = s"$base/process", method = HttpMethods.POST, entity =  data)
  }
/*
  def unmarshalFlow[Input, T](implicit um: Unmarshaller[HttpResponse, T]): Flow[Input, Future[T], NotUsed] = Flow[Input].map{
    case (Success(req), time) => Unmarshal(req).to[T]
    case (Failure(exception), time) => Future.failed(exception)
  }
  */


  protected val simulationStatusRequestFlow: Flow[Token, (Token, HttpRequest), NotUsed] = Flow[Token].map{  case token => token -> HttpRequest(uri = s"$base/process/$token", method = HttpMethods.GET) }

}
