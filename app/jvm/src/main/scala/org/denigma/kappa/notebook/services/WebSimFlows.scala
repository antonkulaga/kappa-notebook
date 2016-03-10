package org.denigma.kappa.notebook.services

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.stream.scaladsl._
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.denigma.kappa.WebSim
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._ //NOTE: sole IDEs like Intellij think that import is unused while it is used

/**
  * Basic idea of the API is that we create a flow for each type of request to websim API, then those flows are applied to http pool
  */
trait WebSimFlows extends CirceSupport {

//  implicit def context: ExecutionContextExecutor

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
