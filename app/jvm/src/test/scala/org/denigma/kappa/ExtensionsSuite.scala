package org.denigma.kappa

import java.io.InputStream

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.pipe
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.testkit.javadsl.TestSink
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
class ExtensionsSuite extends BasicSuite {

  "Extensions" should {
    "Provide an ability to inclusively collect data" in {
      val probe = TestProbe()
      val source = Source(1 to 10) via FlowUpTo[Int](_ >= 4)
      val expRes: Seq[Int] = Seq(1,2,3,4)
      source.runWith(Sink.seq).pipeTo(probe.ref)
      probe.expectMsg(expRes)
    }

    "Stop ticking" in {
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

    "Will stop ticking" in {

      val fl =  Flow[Int].map(i=>i * 10)
      val other = Flow[Int].map(i => i + "!" )
      val test = fl.inputZipWith(other)((a, b)=> (a, b))
      val sink = TestSink.probe[(Int, String)](system)
      Source(0 to 10).via(test)
        .runWith(sink)
        .request(1)
        .expectNext((0, "0!"))
        .request(1)
        .expectNext((1, "10!"))
        .request(1)
        .expectNext((2, "20!"))
    }

  }
}
