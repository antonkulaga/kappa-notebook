package org.denigma.kappa.notebook.services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import pprint.PPrint.PPrint

import scala.concurrent.duration._
import scala.util._

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
