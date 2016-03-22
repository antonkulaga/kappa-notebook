package org.denigma.kappa

import java.io.InputStream

import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.pipe
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestProbe
import akka.util.Timeout
import org.denigma.kappa.WebSim.SimulationStatus
import org.denigma.kappa.notebook.services.WebSimClient
import org.scalatest.concurrent.Futures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Tests quering WebSim
  * to run this test make sure that WebSim server is up and running
  */
class WebSimSuite extends WordSpec with Matchers with ScalatestRouteTest with Futures with BeforeAndAfterAll {

  implicit val duration: FiniteDuration = 800 millis

  implicit val timeout:Timeout = Timeout(duration)

  val server = new WebSimClient()

  def read(res: String) = {
    val stream : InputStream = getClass.getResourceAsStream(res)
    scala.io.Source.fromInputStream( stream ).getLines
  }

  "WebSim" should {

    "return a version number" in {

      val probe = TestProbe()
      server.getVersion().pipeTo(probe.ref)
      probe.expectMsgPF(duration * 2) {
        case WebSim.VersionInfo(build, "v1") if build.contains("Kappa Simulator") =>
      }
    }

    "run simulation and get token" in {
      val probeToken = TestProbe()
      val probeList = TestProbe()

      // The string argument given to getResource is a path relative to
      // the resources directory.

      val abc = read("/abc.ka").reduce(_ + "\n" + _)
      //server.getVersion().pipeTo(probe.ref)
      val params = WebSim.RunModel(abc, 1000, max_events = Some(10000))
      val tokenFut =  server.launch(params).pipeTo(probeToken.ref)

      probeToken.expectMsgPF(duration * 2) {
        case token: Int =>
          println(s"MODEL TOKEN IS " + token)
          server.getRunning().pipeTo(probeList.ref)
          probeList.expectMsgPF(duration * 3) {
            case arr: Array[Int] if arr.contains(token) => println(s"tokens are : [${arr.toList.mkString}]")
          }
      }

    }

    "run simulation and get results" in {
      val probe = TestProbe()
      val abc = read("/abc.ka").reduce(_ + "\n" + _)
      //server.getVersion().pipeTo(probe.ref)
      val params = WebSim.RunModel(abc, 1000, max_events = Some(10000))
      server.launch(params) flatMap{
        case token => server.resultByToken(token)
      } pipeTo probe.ref

      probe.expectMsgPF(duration * 2) {
        case results: WebSim.SimulationStatus =>
          val charts = results.plot map {
            case plot => plot.observables.map(o=>o.time->o.values.toList.mkString)
          } getOrElse Array[(Double, String)]()
      }
    }

    "run streamed results" in {
      val probe = TestProbe()
      val abc = read("/abc.ka").reduce(_ + "\n" + _)
      //server.getVersion().pipeTo(probe.ref)
      val params = WebSim.RunModel(abc, 100, max_events = Some(10000))
      val fut: Future[Seq[SimulationStatus]] = Source.single(params).via(server.modelResultsFlow(1, 100 millis).map(_._2)).runWith(Sink.seq)//.runWithStreamingFlatten(params, Sink.seq, 100 millis)
      fut pipeTo probe.ref
      probe.expectMsgPF(duration * 20) {
        case results: Seq[SimulationStatus] if results.nonEmpty && results.last.percentage == 100 =>
          //println(s"SIMULATION HAS RESULTS :\n"+results)
          println(s"Number of results is ${results.length}")
          println("RESULTS: "+ results.toList.mkString("\n=====================\n"))
      }
      //server.run(params) flatMap{ case token => server.getResult(token) }
    }


  }
  protected override def afterAll() = {
    Http().shutdownAllConnectionPools().onComplete{ _ =>
      system.terminate()
    }
  }
}