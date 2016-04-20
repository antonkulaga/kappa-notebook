package org.denigma.kappa
import java.io.InputStream

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.pipe
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestProbe
import akka.util.Timeout
import org.denigma.kappa.notebook.services.WebSimClient
import org.scalatest.concurrent.Futures
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpec}

import scala.collection.immutable._
import scala.concurrent.Future
import scala.concurrent.duration._
import extensions._
/**
  * Created by antonkulaga on 31/03/16.
  */
class BasicSuite extends WordSpec with Matchers with ScalatestRouteTest with Futures with Inside with BeforeAndAfterAll{

  implicit val duration: FiniteDuration = 1 second

  implicit val timeout:Timeout = Timeout(duration)
}

import java.io.InputStream

import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
class BasicKappaSuite extends BasicSuite with KappaRes {

}