package org.denigma.kappa

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.pattern.pipe
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestProbe
import org.denigma.kappa.messages.{RunModel, SimulationStatus, VersionInfo}
import org.denigma.kappa.notebook.services._

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util._

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
        case VersionInfo(build, "v1") if build.contains("Kappa Simulator") =>
      }
    }


    "run simulation and get token" in {
      val probeToken = TestProbe()
      val probeList = TestProbe()

      // The string argument given to getResource is a path relative to
      // the resources directory.

      //server.getVersion().pipeTo(probe.ref)
      val model = abc
      val params = RunModel(model, Some(1000), max_events = Some(10000))
      val tokenFut =  server.launch(params).pipeTo(probeToken.ref)

      probeToken.expectMsgPF(duration * 2) {
        case Left(token: Int) =>
          //println(s"MODEL TOKEN IS " + token)
          server.getRunning().pipeTo(probeList.ref)
          probeList.expectMsgPF(duration * 3) {
            case arr: Array[Int] if arr.contains(token) => println(s"tokens are : [${arr.toList.mkString(" ")}]")
          }
      }
    }

   "run wrong simulation and get message" in {
     val probeToken = TestProbe()
     val probeList = TestProbe()

     val model = abc
       .replace("A(x),B(x)", "A(x&*&**),*(B(&**&x)")
       .replace("A(x!_,c),C(x1~u)", "zafzafA(x!_,c),azfC(x1~u)") //note: right now sees only one error

     val params = messages.RunModel(model, Some(1000), max_events = Some(10000))
     val tokenFut: Future[Either[server.Token, Array[String]]] = server.launch(params).pipeTo(probeToken.ref)

     probeToken.expectMsgPF(duration * 2) {
       case Right(msg: Array[String]) =>
         //println("cannot launch simulation with the following error:")
         //println(msg.toList.mkString("\n"))
     }
   }

    "run simulation, get first result and " in {
      val probeToken = TestProbe()
      val model = abc
      val params = messages.RunModel(model, Some(1000), max_events = Some(1000000))
      val tokenFut: Future[Either[server.Token, Array[String]]] = server.launch(params).pipeTo(probeToken.ref)

      val token = probeToken.expectMsgPF(duration * 2) {  case Left(t: Int) => t  }
      val source =  Source.single(token)
      val simSink: Sink[(flows.Token, SimulationStatus), Probe[(flows.Token, SimulationStatus)]] = TestSink.probe[(flows.Token, SimulationStatus)]
      val s: Source[(flows.Token, SimulationStatus), NotUsed] = source.via(flows.simulationStatusFlow)
      val tok: Int = s.runWith(simSink).request(1).expectNextPF{
        case (t: Int, status: SimulationStatus ) =>
          println(t -> status)
          t
      }

      val testRun = TestSink.probe[Array[Int]]
      val ps: Array[Int] = Source.single(Unit).via(server.flows.running).runWith(testRun).request(1).expectNext()
      ps.contains(tok) shouldEqual(true)

      val respSink = TestSink.probe[HttpResponse]

      //val del= source.via(flows.stopFlow)
      val delSink = TestSink.probe[Try[HttpResponse]]
      val del = flows.stopRequestFlow.via(flows.timePool).map(_._1)
      Source.single[Int](tok).via(del).runWith(delSink).request(1).expectNextPF {
        case res =>
          println("result " + res)
      }
    }

    "run streamed tabs" in {
      val tokenSink = TestSink.probe[(Either[Int, Array[String]], RunModel)]
      val params = messages.RunModel(abc, Some(100), max_events = Some(10000))
      val launcher = Source.single(params).via(flows.tokenFlow).runWith(tokenSink)
      val (token, model) = launcher.request(1).expectNextPF{ case (Left(t: Int), mod) =>  t -> mod }
      val simSource = Source.single(token).via(flows.syncSimulationStream)
      val probe = TestProbe()
      simSource.runWith(Sink.seq).pipeTo(probe.ref)
      val results = probe.expectMsgPF(800 millis){
        case res: Seq[(Int, SimulationStatus)]=> res
      }
      results.nonEmpty shouldEqual true
      results.last._2.percentage >= 100.0 shouldBe true
    }


     "run simulation and get results" in {
       val probe = TestProbe()
       val params = messages.RunModel(abc, Some(1000), max_events = Some(10000))
       server.run(params).map(_._1).pipeTo(probe.ref)
       probe.expectMsgPF(800 millis){
         case  Left( (token: Int, sim: SimulationStatus)) if sim.percentage>=100.0  =>
           //println(sim)
       }
     }
 }

 protected override def afterAll() = {
   Http().shutdownAllConnectionPools().onComplete{ _ =>
     system.terminate()
   }
 }
}