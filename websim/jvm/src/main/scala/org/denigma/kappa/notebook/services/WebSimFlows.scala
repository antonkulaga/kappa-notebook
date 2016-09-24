package org.denigma.kappa.notebook.services

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.stream.scaladsl._
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.generic.auto._
import io.circe.syntax._
import org.denigma.kappa.messages.ServerMessages.LaunchModel
import org.denigma.kappa.messages.WebSimMessages._
import pprint.PPrint

/**
  * Basic idea of the API is that we create a flow for each type of request to websim API, then those flows are applied to http pool
  */
trait WebSimFlows extends CirceSupport{


  protected def debug[T: PPrint](something: T) = {
    pprint.pprintln(something)
  }

  type Token = Int

  def base: String

  protected val versionRequestFlow: Flow[Any, HttpRequest, NotUsed] = Flow[Any].map(any => HttpRequest(uri = s"$base/version"))

  protected val runningRequestFlow: Flow[Any, HttpRequest, NotUsed] = Flow[Any].map(any => HttpRequest(uri = s"$base/process", method = HttpMethods.GET))

  /**
    * Flow where you provide Running configurations to get Model with request in return
    */
  protected val parseRequestFlow: Flow[ParseCode, HttpRequest, NotUsed] = Flow[ParseCode].map{
    case ParseCode(code) =>
      val data = Uri(s"$base/parse").withQuery(Uri.Query(Map("code"->code)))
      HttpRequest(uri = data, method = HttpMethods.GET)
  }

  protected val parseModelFlow: Flow[LaunchModel, HttpRequest, NotUsed] = Flow[LaunchModel].map{ m =>
      val data = Uri(s"$base/parse").withQuery(Uri.Query(Map("code"->m.fullCode)))
      HttpRequest(uri = data, method = HttpMethods.GET)
  }

  /**
    * Flow where you provide Running configurations to get Model with request in return
    */
  protected val runModelRequestFlow: Flow[LaunchModel, HttpRequest, NotUsed] = Flow[LaunchModel].map{ model =>
      val json = model.parameters.asJson.noSpaces
      //debug(json)
      val data = HttpEntity(ContentTypes.`application/json`, json)
      HttpRequest(uri = s"$base/process", method = HttpMethods.POST, entity =  data)
  }

  protected val simulationStatusRequestFlow: Flow[Token, HttpRequest, NotUsed] =
    Flow[Token].map{  token =>  HttpRequest(uri = s"$base/process/$token", method = HttpMethods.GET) }

  protected def command(name: String) = Flow[Token].map{ token =>
    HttpRequest(uri = s"$base/process/$token/$name", method = HttpMethods.GET)
  }
  ///perturbate|/pause|/continue
  val perturbateFlow: Flow[Token, HttpRequest, NotUsed] = command("pause")
  val pauseRequestFlow: Flow[Token, HttpRequest, NotUsed] = command("pause")
  val continueFlow: Flow[Token, HttpRequest, NotUsed] = command("continue")


  val stopRequestFlow: Flow[Token, HttpRequest, NotUsed] = Flow[Token].map{ token =>
    HttpRequest(uri = s"$base/process/$token", method = HttpMethods.DELETE)
  }

}
