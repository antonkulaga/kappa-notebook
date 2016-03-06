package org.denigma.kappa

import java.io.InputStream

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.pipe
import akka.testkit.TestProbe
import akka.util.Timeout
import org.denigma.kappa.notebook.services.WebSimClient
import org.scalatest.concurrent.Futures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class WebSimSuite extends WordSpec with Matchers with ScalatestRouteTest with Futures {

  implicit val duration: FiniteDuration = 800 millis

  implicit val timeout:Timeout = Timeout(duration)
  /*
  val smallRoute =
    get {
      pathSingleSlash {
        complete {
          "Captain on the bridge!"
        }
      } ~
        path("ping") {
          complete("PONG!")
        }
    }

  "The service" should {
    "return a greeting for GET requests to the root path" in {
      // tests:
      Get() ~> smallRoute ~> check {
        responseAs[String] shouldEqual "Captain on the bridge!"
      }
    }
  }
  */

  def read(res: String) = {
    val stream : InputStream = getClass.getResourceAsStream(res)
    scala.io.Source.fromInputStream( stream ).getLines
  }

  "WebSim" should {
    val server = new WebSimClient()
    "return a version number" in {

      val probe = TestProbe()
      server.getVersion().pipeTo(probe.ref)
           probe.expectMsgPF(duration * 2) {
        case WebSim.VersionInfo(build, "v1") if build.contains("Kappa Simulator") =>
      }
    }

    "run simulation and get token" in {
      val server = new WebSimClient()
      val probeToken = TestProbe()
      val probeList = TestProbe()

      // The string argument given to getResource is a path relative to
      // the resources directory.

      val abc = read("/abc.ka").reduce(_ + "\n" + _)
      //server.getVersion().pipeTo(probe.ref)
      val params = WebSim.RunModel(abc, 1000, max_events = Some(10000))
      val tokenFut =  server.run(params).pipeTo(probeToken.ref)

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
      val server = new WebSimClient()
      val probe = TestProbe()

      // The string argument given to getResource is a path relative to
      // the resources directory.

      val abc = read("/abc.ka").reduce(_ + "\n" + _)
      //server.getVersion().pipeTo(probe.ref)
      val params = WebSim.RunModel(abc, 1000, max_events = Some(10000))
      server.run(params) flatMap{
        case token => server.getResult(token)
      } pipeTo probe.ref

      probe.expectMsgPF(duration * 2) {
        case results: WebSim.SimulationStatus =>
          //println(s"SIMULATION HAS RESULTS :\n"+results)
          println("PERCENTS DONE: "+ results.percentage)
          val charts = results.plot map {
            case plot => plot.observables.map(o=>o.time->o.values.toList.mkString)
          } getOrElse Array[(Double, String)]()
          println("charts are: ")
          charts.foreach(println(_))

      }

    }

  }
}