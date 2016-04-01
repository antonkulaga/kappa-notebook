package org.denigma.kappa


import java.io.InputStream

import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.pipe
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestProbe
import akka.util.Timeout
import org.denigma.kappa.WebSim.{KappaPlot, SimulationStatus}
import org.denigma.kappa.notebook.services._
import org.scalatest.concurrent.Futures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration._
/*
/**
  * Tests quering WebSim
  * to run this test make sure that WebSim server is up and running
  */
class ParameterSearchSuite extends WordSpec with Matchers with ScalatestRouteTest with Futures with BeforeAndAfterAll {

  implicit val duration: FiniteDuration = 800 millis

  implicit val timeout: Timeout = Timeout(duration)


  val server = new ParameterSearcher()

  def read(res: String) = {
    val stream : InputStream = getClass.getResourceAsStream(res)
    scala.io.Source.fromInputStream( stream ).getLines
  }

    "Parameter searchers" should {

      "run several" in {

        val probe = TestProbe()
        val abc = read("/abc.ka").reduce(_ + "\n" + _)
        //server.getVersion().pipeTo(probe.ref)
        val params = WebSim.RunModel(abc, 100, max_events = Some(10000))
        val fut: Future[Seq[(TokenPoolMessage, SimulationStatus)]] = server.run(Seq(params, params, params)) //Source.single(params).via(server.modelResultsFlow(1, 100 millis).map(_._2)).runWith(Sink.seq)//.runWithStreamingFlatten(params, Sink.seq, 100 millis)
        fut pipeTo probe.ref
        probe.expectMsgPF(duration * 20) {
          case results: Seq[(TokenPoolMessage, SimulationStatus)] if results.length == 3 && results.forall(p => p._2.percentage >= 100.0 && p._2.plot.isDefined) =>
            val sims = results.map { case (token, res) => res }
            val plots: List[KappaPlot] = sims.map { case s =>
              s.plot.get
            }.toList
            //println("length is " + plots.length)
            //if(plots.isEmpty) println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            //pprint.pprintln(s"===notFinished ${s.notFinished} and ${s.percentage} and ${s.is_running}=====")
            //pprint.pprintln(s.plot)
            //pprint.pprintln("===================")

            //val m = KappaMatrix(plots.toList)
            //println("\n======ROW STD====================")
            //pprint.pprintln(m.rowStd.length)
            //pprint.pprintln(m.rowStd)
        }

      }


          /*
      "run parameter estimation" in {
        import breeze.linalg._
        val probe = TestProbe()
        val abc = read("/abc.ka").reduce(_ + "\n" + _)
        //server.getVersion().pipeTo(probe.ref)
        val params = WebSim.RunModel(abc, 100, max_events = Some(10000))
        //

        val fut: Future[Seq[(TokenPoolMessage, SimulationStatus)]] = server.run(Seq(params, params, params))//Source.single(params).via(server.modelResultsFlow(1, 100 millis).map(_._2)).runWith(Sink.seq)//.runWithStreamingFlatten(params, Sink.seq, 100 millis)
        fut pipeTo probe.ref
        probe.expectMsgPF(duration * 20) {
          case results: Seq[(TokenPoolMessage, SimulationStatus)] if results.length == 3 && results.forall(p => p._2.percentage >= 100.0 && p._2.plot.isDefined) =>
            results.map{ case (token, res)=>res.plot.get }
        }
      }
      */

    }


  protected override def afterAll() = {
    Http().shutdownAllConnectionPools().onComplete{ _ =>
      system.terminate()
    }
  }
}
*/