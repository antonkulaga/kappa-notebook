package org.denigma.kappa


import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.pattern.pipe
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestProbe
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.services._

import scala.collection.immutable.Seq
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util._
import WebSimMessages._
import org.denigma.kappa.messages.ServerMessages.{LaunchModel, ServerConnection}

/**
  * Tests quering WebSim
  * to run this test make sure that WebSim server is up and running
  */
class WebSimSuite extends BasicKappaSuite {

  val server = new WebSimClient(ServerConnection.default)
  val flows = server.flows

  "WebSim" should {

    "have working version flow" in {
      val p = TestSink.probe
      val source = Source.single(Unit)
      source.via(flows.versionFlow)
        .runWith(TestSink.probe[Version])
        .request(1)
        .expectNextPF{ case v: Version => v }
    }

    "return a version number" in {
      val probe = TestProbe()
      server.getVersion().pipeTo(probe.ref)
      probe.expectMsgPF(duration * 2) {
        case Version(build, "v1") if build.contains("Kappa Simulator") =>
      }
    }

    "parse simulations" in {
      val probeToken = TestProbe()
      val probeList = TestProbe()

      val model = abc
      server.parse(ParseCode(model)).pipeTo(probeToken.ref)

      probeToken.expectMsgPF(duration * 2) {
        case Left(result) =>
        //println(result)
      }

      val wrongModel = abc
        .replace("A(x),B(x)", "A(x&*&**),*(B(&**&x)")
        .replace("A(x!_,c),C(x1~u)", "zafzafA(x!_,c),azfC(x1~u)") //note: right now sees only one error

      server.parse(ParseCode(wrongModel)).pipeTo(probeToken.ref)

      probeToken.expectMsgPF(duration * 2) {
        case Right(msg) =>
          msg
        //println(msg)
      }
    }

    "run simulation and get token" in {
      val probeToken = TestProbe()
      val probeList = TestProbe()
      val model = abc
      val wrongModel = model
        .replace("A(x),B(x)", "A(x&*&**),*(B(&**&x)")
        .replace("A(x!_,c),C(x1~u)", "zafzafA(x!_,c),azfC(x1~u)") //note: right now sees only one error

      val wrongParams = LaunchModel(List("abc" -> wrongModel), 0.1, max_events = Some(1000000))
      server.launch(wrongParams).pipeTo(probeToken.ref)
      probeToken.expectMsgPF(duration * 2) {
        case (Right(msg), model) =>
          println("launching wrong model works well")
      }

      val params = LaunchModel(List("abc"->model), 0.1, max_events = Some(10000))
      server.launch(params).pipeTo(probeToken.ref)

      probeToken.expectMsgPF(duration * 2) {
        case (Left((token: Int, mp: ContactMap)), mod) =>
          server.getRunning().pipeTo(probeList.ref)
          probeList.expectMsgPF(duration * 3) {
            case arr: Array[Int] if arr.contains(token) => //println(s"tokens are : [${arr.toList.mkString(" ")}]")
          }
      }
    }

    "run simulation, get first result and stop" in {
      val probeToken = TestProbe()
      val model = abc
      val params = LaunchModel(List("abc"->model), 0.1, max_events = Some(1000000))
      val tokenFut = server.launch(params).pipeTo(probeToken.ref)
      val token = probeToken.expectMsgPF(duration * 2) {  case (Left((t: Int, mp: ContactMap)), mod) => t  }
      val source =  Source.single(token)
      val simSink: Sink[(flows.Token, SimulationStatus), Probe[(flows.Token, SimulationStatus)]] = TestSink.probe[(flows.Token, SimulationStatus)]
      val s: Source[(flows.Token, SimulationStatus), NotUsed] = source.via(flows.simulationStatusFlow)
      val tok: Int = s.runWith(simSink).request(1).expectNextPF{
        case (t: Int, status: SimulationStatus ) =>
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
      }
    }

    "run simulation extended" in {

      val probeToken = TestProbe()
      val model = abc
      val params = LaunchModel(List("abc"->model), 0.1, max_events = Some(100000000))
      val tokenFut: Future[server.flows.Runnable[server.flows.TokenContactResult]] = server.launch(params).pipeTo(probeToken.ref)
      val token = probeToken.expectMsgPF(duration * 2) {
        case (Left((t: Int, result)), m) => t
      }
      val source =  Source.single(token)
      val simSink: Sink[(flows.Token, SimulationStatus), Probe[(flows.Token, SimulationStatus)]] = TestSink.probe[(flows.Token, SimulationStatus)]
      val s: Source[(flows.Token, SimulationStatus), NotUsed] = source.via(flows.simulationStatusFlow)
      val tok: Int = s.runWith(simSink).request(1).expectNextPF{
        case (t: Int, status: SimulationStatus ) =>
          t
      }

      val testRun = TestSink.probe[Array[Int]]
      val ps: Array[Int] = Source.single(Unit).via(server.flows.running).runWith(testRun).request(1).expectNext()
      ps.contains(tok) shouldEqual(true)

      val respSink = TestSink.probe[HttpResponse]

      //val del= source.via(flows.stopFlow)
      val delSink = TestSink.probe[Try[HttpResponse]]
      val del = flows.stopRequestFlow.via(flows.timePool).map(_._1)

      Await.result(server.simulationStatusByToken(token), 400 millis).is_running shouldEqual true
      Source.single[Int](tok).via(del).runWith(delSink).request(1).expectNextPF {
        case res =>
      }
      Await.result(server.simulationStatusByToken(token), 400 millis).is_running shouldEqual false

    }

    "run streamed" in {
      val tokenSink = TestSink.probe[flows.Runnable[flows.TokenContactResult]]
      val params = LaunchModel(List("abc"->abc), 0.1, max_events = Some(5000))
      val launcher: Probe[flows.Runnable[flows.TokenContactResult]] = Source.single(params).via(flows.runModelFlow).runWith(tokenSink)
      val (token, model) = launcher.request(1).expectNextPF{
        case (Left((t: Int, cm)), mod) =>  t -> mod
      }
      val simSource = Source.single(token).via(flows.syncSimulationStream)
      val probe = TestProbe()
      simSource.runWith(Sink.seq).pipeTo(probe.ref)
      val results = probe.expectMsgPF(5 seconds){
        case res: Seq[(Int, SimulationStatus)]=> res
      }
      results.nonEmpty shouldEqual true
      results.last._2.percentage >= 100.0 shouldBe true
    }


    "run simulation and get results" in {

      val probe = TestProbe()
      val params = LaunchModel(List("abc"->abcFlow), 0.1, max_events = Some(1000))

      server.run(params).map(_._1).pipeTo(probe.ref)

      probe.expectMsgPF(5 seconds) {
        case Left((token: Int, sim: SimulationStatus, mp)) if sim.percentage >= 100.0 =>
        //Token, SimulationStatus, ContactMap
      }
    }


    "stop simulation" in {
      val params = LaunchModel(List("abc"-> abcFlow), 0.1, max_events = Some(10000000))
      val Left((token, _))= Await.result(server.launch(params), 500 millis)._1
      Await.result(server.simulationStatusByToken(token), 400 millis).is_running shouldEqual true
      Await.result(server.stop(token), 1 second)
      Await.result(server.simulationStatusByToken(token), 400 millis).is_running shouldEqual false
    }


    "pause/continue simulation" in {
      //TODO: make complete
      val params = LaunchModel(List("abc"-> abcFlow), 0.1, max_events = Some(10000000))
      val Left((token, _))= Await.result(server.launch(params), 500 millis)._1
      Await.result(server.simulationStatusByToken(token), 400 millis).is_running shouldEqual true
      Await.result(server.pause(token), 1 second)
      Await.result(server.simulationStatusByToken(token), 400 millis).is_running shouldEqual false
      Await.result(server.continue(token), 1 second)
      eventually{  Await.result(server.simulationStatusByToken(token), 400 millis).is_running }
      Await.result(server.stop(token), 1 second)
      Await.result(server.simulationStatusByToken(token), 400 millis).is_running shouldEqual false
    }

    "write snapshots" in {
      val probe = TestProbe()
      val params = LaunchModel(List("snap400.ka"-> snap400), 0.1, max_events = Some(400))

      server.run(params).map(_._1).pipeTo(probe.ref)

      probe.expectMsgPF(3 seconds) {
        case Left((token: Int, sim: SimulationStatus, mp)) if sim.snapshots.nonEmpty && sim.snapshots.head.snap_file == "foo_snapshot" =>
          //println("SNAPSHOTS ARE: \n" + sim.snapshots)
        //Token, SimulationStatus, ContactMap
      }
    }
  }

  protected override def afterAll() = {
    Http().shutdownAllConnectionPools().onComplete{ _ =>
      system.terminate()
    }
  }
}