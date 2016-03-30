package org.denigma.kappa

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by antonkulaga on 3/29/16.
  */
class Tester(implicit system: ActorSystem, materializer: ActorMaterializer) {

  implicit def dispatcher = system.dispatcher

  def wait[T](fut: Future[T]): T = Await.result(fut, 2 seconds)

  def open(method: HttpMethod, path: String, params: (String, String)*): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(uri = Uri(path).withQuery(Query(params:_*))))

  def get(path: String, params: (String, String)*): Future[HttpResponse] =
    open(HttpMethods.GET, path, params:_*)

  def post(path: String, params: (String, String)*): Future[HttpResponse] =
    open(HttpMethods.GET, path, params:_*)

}
