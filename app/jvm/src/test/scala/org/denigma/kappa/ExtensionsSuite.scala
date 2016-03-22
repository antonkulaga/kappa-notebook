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
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.collection.immutable._
import scala.concurrent.Future
import scala.concurrent.duration._
import extensions._

/**
  * Created by antonkulaga on 08/03/16.
  */
class ExtensionsSuite extends WordSpec with Matchers with ScalatestRouteTest with Futures with BeforeAndAfterAll {
  implicit val duration: FiniteDuration = 800 millis

  implicit val timeout:Timeout = Timeout(duration)

  "Extensions" should {
    "Provide an ability to inclusively collect data" in {
      val probe = TestProbe()
      val source = Source(1 to 10) via FlowUpTo[Int](_ >= 4)
      val expRes: Seq[Int] = Seq(1,2,3,4)
      source.runWith(Sink.seq).pipeTo(probe.ref)
      probe.expectMsg(expRes)
    }

    "Will stop ticking" in {
      val probe = TestProbe()
      var a = 0
      val t = Source.tick(0 millis, 100 millis, true)
      val counting = t.map(s => {a = a + 1; a})
      val fl = counting via FlowUpTo(_ >= 4)
      fl.runWith(Sink.seq)
        .pipeTo(probe.ref)
      val expRes:Seq[Int] = Seq(1,2,3,4)
      probe.expectMsg(expRes)
    }

  }
}
