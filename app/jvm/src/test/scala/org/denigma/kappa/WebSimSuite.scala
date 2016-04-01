package org.denigma.kappa

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.pattern.pipe
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestProbe
import org.denigma.kappa.WebSim.{SimulationStatus, VersionInfo}
import org.denigma.kappa.notebook.services.WebSimClient

import scala.concurrent.Future
import scala.util.Either

/**
  * Tests quering WebSim
  * to run this test make sure that WebSim server is up and running
  */
class WebSimSuite extends BasicKappaSuite {

  val server = new WebSimClient()
  val flows = server.flows

  "WebSim" should {

    "have working version flow" in {
      val p = TestSink.probe
      val source = Source.single(Unit)
      source.via(flows.versionFlow)
        .runWith(TestSink.probe[VersionInfo])
        .request(1)
        .expectNextPF{ case v:VersionInfo => v }
    }

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

      //server.getVersion().pipeTo(probe.ref)
      val model = abc
      val params = WebSim.RunModel(model, 1000, max_events = Some(10000))
      val tokenFut =  server.launch(params).pipeTo(probeToken.ref)

      probeToken.expectMsgPF(duration * 2) {
        case Left(token: Int) =>
          //println(s"MODEL TOKEN IS " + token)
          server.getRunning().pipeTo(probeList.ref)
          probeList.expectMsgPF(duration * 3) {
            case arr: Array[Int] if arr.contains(token) => println(s"tokens are : [${arr.toList.mkString}]")
          }
      }
    }

   "run wrong simulation and get message" in {
     val probeToken = TestProbe()
     val probeList = TestProbe()

     val model = abc
       .replace("A(x),B(x)", "A(x&*&**),*(B(&**&x)")
       .replace("A(x!_,c),C(x1~u)", "zafzafA(x!_,c),azfC(x1~u)") //note: right now sees only one error

     val params = WebSim.RunModel(model, 1000, max_events = Some(10000))
     val tokenFut: Future[Either[server.Token, Array[String]]] = server.launch(params).pipeTo(probeToken.ref)

     probeToken.expectMsgPF(duration * 2) {
       case Right(msg: Array[String]) =>
         //println("cannot launch simulation with the following error:")
         println(msg.toList.mkString("\n"))
     }
   }

    "run simulation, get first result and " in {
      val probeToken = TestProbe()
      val model = abc
      val params = WebSim.RunModel(model, 1000, max_events = Some(10000))
      val tokenFut: Future[Either[server.Token, Array[String]]] = server.launch(params).pipeTo(probeToken.ref)

      probeToken.expectMsgPF(duration * 2) {
        case Left(token: Int) =>
          val source =  Source.single(token)
          val simSink: Sink[(flows.Token, SimulationStatus), Probe[(flows.Token, SimulationStatus)]] = TestSink.probe[(flows.Token, SimulationStatus)]
          val s: Source[(flows.Token, SimulationStatus), NotUsed] = source.via(flows.simulationStatusFlow)
          s.runWith(simSink).request(1).expectNextPF{
            case (t, status) =>
              println(t->status)
          }
          val testRun: Sink[Array[Int], Probe[Array[Int]]] = TestSink.probe[Array[Int]]
          /*
          Source.single(Unit).via(flows.running).runWith(testRun)
              .request(1).expectNext(token)
*/

      }

    }
/*
    "parses for errors" in {
      val probeToken = TestProbe()
      val probeList = TestProbe()

      val model = abc
        .replace("A(x),B(x)", "A(x&*&**),*(B(&**&x)")
        .replace("A(x!_,c),C(x1~u)", "zafzafA(x!_,c),azfC(x1~u)") //note: right now sees only one error

      val params = WebSim.RunModel(model, 1000, max_events = Some(10000))
      val tokenFut = server.launch(params).pipeTo(probeToken.ref)

      probeToken.expectMsgPF(duration * 2) {
        case Right(msg: Array[String]) =>
          //println("cannot launch simulation with the following error:")
          println(msg.toList.mkString("\n"))
      }
    }


   /*
         "runmulation and get results" in {
           val probe = TestProbe()
           val abc = read("/abc.ka").reduce(_ + "\n" + _)
           val params = WebSim.RunModel(abc, 1000, max_events = Some(10000))
           server.launch(params) flatMap{
             case token => server.resultByToken(token)
           } pipeTo probe.ref


           probe.expectMsgPF(duration * 2) {
             case results: WebSim.SimulationStatus =>
               /*
               val charts = results.plot map {
                 case plot => plot.observables.map(o=>o.time->o.values.toList.mkString)
               } getOrElse Array[(Double, String)]()
               */
           }
         }

        "run streamed results" in {
          val probe = TestProbe()
          val abc = read("/abc.ka").reduce(_ + "\n" + _)
          //server.getVersion().pipeTo(probe.ref)
          val params = WebSim.RunModel(abc, 100, max_events = Some(10000))
          val fut: Future[Seq[SimulationStatus]] = Source.single(params).via(server.makeModelResultsFlow(1, 100 millis).map(_._2)).runWith(Sink.seq)//.runWithStreamingFlatten(params, Sink.seq, 100 millis)
          fut pipeTo probe.ref
          probe.expectMsgPF(duration * 20) {
            case results: Seq[SimulationStatus] if results.nonEmpty && results.last.percentage == 100 =>
          }
          //server.run(params) flatMap{ case token => server.getResult(token) }
        }

         "run wrong models" in {
           val probe = TestProbe()
           val abc = read("/abc.ka").reduce(_ + "\n" + _).replace("A(x),B(x)", "A(x&*&**),*(B(&**&x)")
           val params = WebSim.RunModel(abc, 1000, max_events = Some(10000))
           server.launch(params) flatMap{
             case token =>
               val result = server.resultByToken(token)
               println("============================")
               println(result)
               result
           } pipeTo probe.ref


           probe.expectMsgPF(duration * 2) {
             //case results: WebSim.SimulationStatus =>
             case result =>
               println("============================")
               println(result)
             /*
             val charts = results.plot map {
               case plot => plot.observables.map(o=>o.time->o.values.toList.mkString)
             } getOrElse Array[(Double, String)]()
             */
           }
         }
       */*/
 }

 protected override def afterAll() = {
   Http().shutdownAllConnectionPools().onComplete{ _ =>
     system.terminate()
   }
 }
}