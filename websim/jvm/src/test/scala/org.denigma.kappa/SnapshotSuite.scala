package org.denigma.kappa

import org.scalacheck.Gen
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
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{KappaSnapshot, Pattern}
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

/**
  * Created by antonkulaga on 10/11/16.
  */
class SnapshotSuite extends BasicKappaSuite {


  val server = new WebSimClient(ServerConnection.default)
  val flows = server.flows

  "Snapshots" should {

    "be received from websim" in {
      val probe = TestProbe()
      val params = LaunchModel(List("snap400.ka"-> snap400), Some(400), max_events = Some(400))

      server.run(params).map(_._1).pipeTo(probe.ref)

      probe.expectMsgPF(3 seconds) {
        case Left((token: Int, sim: SimulationStatus, mp)) if sim.snapshots.nonEmpty && sim.snapshots.head.snap_file == "foo_snapshot" =>
          //println("SNAPSHOTS ARE: \n" + sim.snapshots)
        //Token, SimulationStatus, ContactMap
      }

    }

    lazy val dna1 = KappaModel.Pattern(List(KappaModel.Agent("DNA", position = 0)))
    lazy val dna2 = KappaModel.Pattern(List(
      KappaModel.Agent("DNA", Set(KappaModel.Site("down", Set.empty, Set("1"))), 0),
      KappaModel.Agent("DNA", Set(KappaModel.Site("up", Set.empty, Set("1"))), 1)
    ))
    lazy val dna3 =  KappaModel.Pattern(List(
      KappaModel.Agent("DNA", Set(KappaModel.Site("down", Set.empty, Set("1"))), 0),
      KappaModel.Agent("DNA", Set(KappaModel.Site("up", Set.empty, Set("1")), KappaModel.Site("down", Set.empty, Set("2"))), 1),
      KappaModel.Agent("DNA", Set(KappaModel.Site("up", Set.empty, Set("2"))), 2)
    )
    )

    "be tested by generated DNA" in {
      generateDna(1, Pattern.empty) shouldEqual dna1
      generateDna(2, Pattern.empty) shouldEqual dna2
      generateDna(3, Pattern.empty) shouldEqual dna3
    }

    "have correct embeding" in {

      //agen embeddings
      dna1.agents.head.embedsInto(dna2.agents.tail.head) shouldBe true //less specific dna embeds in more specific
      dna2.agents.tail.head.embedsInto(dna2.agents.head) shouldBe false //but not vice versa
      dna1.embedsInto(dna2) shouldEqual true
      dna2.embedsInto(dna1) shouldEqual false
      dna1.embedsCount(dna2) shouldEqual 2

      dna1.embedsInto(dna3) shouldEqual true
      dna3.embedsInto(dna1) shouldEqual false
      dna1.embedsCount(dna3) shouldEqual 3

      dna2.embedsInto(dna3) shouldEqual true
      dna3.embedsInto(dna2) shouldEqual false
      //dna2.embedsCount(dna3) shouldBe 2

      val mp =  Map( (dna3, 100), (dna2, 200), (dna1, 1000))


      val snap1 = KappaSnapshot("test", 1000, mp)
      snap1.embeddingsOf(dna1) shouldEqual mp
      snap1.embeddingsOf(dna2) shouldEqual  Map( (dna3, 100), (dna2, 200))
      snap1.embeddingsOf(dna3) shouldEqual  Map( (dna3, 100))
    }
  }

  lazy val dnaGenerator: Gen[Pattern] = {
    for {
     length <- Gen.choose(0, 100)
     quantity <- Gen.choose(0, 5000)
   } yield generateDna(length, Pattern.empty)

  }


  def generateDna(length: Int, acc: Pattern = Pattern.empty): Pattern = if(acc.agents.length == length) acc.copy(acc.agents.reverse)
  else
  if(acc.agents.isEmpty){
    generateDna(length, acc.copy(List(KappaModel.Agent("DNA", position = 0))))
  }
  else
  {
    val left = acc.agents.head
    val linkName = acc.agents.length.toString
    val down = KappaModel.Site("down", Set.empty, Set(linkName))
    val newLeft = left.copy(sites = left.sites +down)
    val up = KappaModel.Site("up", Set.empty, Set(linkName))
    val right = KappaModel.Agent("DNA", Set(up), position = left.position + 1)
    generateDna(length, acc.copy(right::newLeft::acc.agents.tail))
  }


  protected override def afterAll() = {
    Http().shutdownAllConnectionPools().onComplete{ _ =>
    system.terminate()
    }
  }
}
