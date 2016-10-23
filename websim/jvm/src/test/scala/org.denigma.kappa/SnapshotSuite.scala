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
import fastparse.core.Parsed
import org.denigma.kappa.messages.ServerMessages.{LaunchModel, ServerConnection}
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{KappaSnapshot, Pattern}
import org.denigma.kappa.parsers.KappaParser
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

/**
  * Created by antonkulaga on 10/11/16.
  */
class SnapshotSuite extends BasicKappaSuite {


  val server = new WebSimClient(ServerConnection.default)
  val flows = server.flows

  "Snapshots" should {

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
      dna1.embedsIntoCount(dna2) shouldEqual 2

      dna1.embedsInto(dna3) shouldEqual true
      dna3.embedsInto(dna1) shouldEqual false
      dna1.embedsIntoCount(dna3) shouldEqual 3

      dna2.embedsInto(dna3) shouldEqual true
      dna3.embedsInto(dna2) shouldEqual false
      dna2.embedsIntoCount(dna3) shouldEqual 2

      val mp =  Map( (dna3, 100), (dna2, 200), (dna1, 1000))


      val snap1 = KappaSnapshot("test", 1000, mp)
      snap1.embeddingsOf(dna1) shouldEqual mp
      snap1.embeddingsOf(dna2) shouldEqual  Map( (dna3, 100), (dna2, 200))
      snap1.embeddingsOf(dna3) shouldEqual  Map( (dna3, 100))
    }

    "convert to KappaModel.Snapshot in a right way" in {
      val ags2 = List(
        (20, List(
          WebSimNode("DNA", List(WebSimSite("down", List((1,0)), Nil))),
          WebSimNode("DNA", List(WebSimSite("up", List((0,0)), Nil)))
        ))
      )
      val snapshot2 = Snapshot("file", 1000, ags2)
      val snap2 = snapshot2.toKappaSnapshot
      snap2.patterns.size shouldEqual snapshot2.agents.size
      snap2.patterns.head shouldEqual(
        (dna2, 20)
        )

      val ags3 = List(
        (30, List(
          WebSimNode("DNA", List(WebSimSite("down", List((1,0)), Nil))),
          WebSimNode("DNA", List(
            WebSimSite("up", List((0,0)), Nil),
            WebSimSite("down", List((2,0)), Nil)
          )),
          WebSimNode("DNA", List(WebSimSite("up", List((1,1)), Nil)))
        ))
      )
      val snapshot3 = Snapshot("file", 1000, ags3)
      val snap3 = snapshot3.toKappaSnapshot
      snap3.patterns.size shouldEqual snapshot3.agents.size
      snap3.patterns.head shouldEqual(
        (dna3, 30)
        )
    }

    "be received from websim" in {
      val probe = TestProbe()
      val params = LaunchModel(List("snap400.ka"-> snap400), Some(400), max_events = Some(400))

      server.run(params).map(_._1).pipeTo(probe.ref)

      val snapshot = probe.expectMsgPF(3 seconds) {
        case Left((token: Int, sim: SimulationStatus, mp)) if sim.snapshots.nonEmpty && sim.snapshots.head.snap_file == "foo_snapshot" => sim.snapshots.head
        //println("SNAPSHOTS ARE: \n" + sim.snapshots)
        //Token, SimulationStatus, ContactMap
      }
      val speciesNum = snapshot.agents.length
      val allNum = snapshot.agents.foldLeft(0){ case (acc, (i, _)) => acc + i }

      def checkDNA(ags: List[WebSimNode], num: Int) = ags.sliding(num, 1).exists(p=>p.length == num &&
        p.head.node_sites.last.site_links.nonEmpty
        && p.last.node_sites.head.site_links.nonEmpty)

      val dna2Num = snapshot.agents.foldLeft(0){ case (acc, (i, p)) => if(checkDNA(p, 2)) acc + i else acc }
      val dna3Num = snapshot.agents.foldLeft(0){ case (acc, (i, p)) => if(checkDNA(p, 3)) acc + i else acc }

      val snap = snapshot.toKappaSnapshot
      snapshot.agents.length shouldEqual snap.patterns.size
      snap.patterns.size shouldEqual speciesNum
      snap.patterns.values.foldLeft(0){ case (acc, i) => acc + i} shouldEqual allNum
      snap.embeddingsOf(dna1).size shouldEqual speciesNum
      //snap.patterns.foreach(p=> pprint.pprintln(p))
      //pprint.pprintln("NUMBER OF SNAP PATTERNS = "+snap.patterns.size)
      val dna2Embeds = snap.embeddingsOf(dna2)

      sumPatterns(dna2Embeds) shouldEqual dna2Num
      val d3 = sumPatterns(snap.embeddingsOf(dna3))
      d3 shouldEqual dna3Num
  }

  "work out of kappa file" in {
    val parser = new KappaParser()

    val ( Parsed.Success(KappaModel.InitCondition(Right(829), dna1), _),
      Parsed.Success(KappaModel.InitCondition(Right(70), dna2), _),
      Parsed.Success(KappaModel.InitCondition(Right(9), dna3), _) ,
      Parsed.Success(KappaModel.InitCondition(Right(1), dna4), _) ) =
    (
      parser.init.parse("%init: 829 DNA(up, down)"),
      parser.init.parse("%init: 70 DNA(up!1, down), DNA(up, down!1)"), //dna2
      parser.init.parse("%init: 9 DNA(up, down!1), DNA(up!1, down!2), DNA(up!2, down)"),
      parser.init.parse("%init: 1 DNA(up, down!1), DNA(up!1, down!2), DNA(up!3, down), DNA(up!2, down!3)")
    )
    val snap = KappaSnapshot("test", 100, Map(
      (dna1, 829), (dna2, 70), (dna3, 9), (dna4, 1)
    ))
    val Parsed.Success(pat1, _) = parser.rulePart.parse("DNA(up!1, down), DNA(up, down!1)")
    pat1.embedsInto(dna1) shouldEqual false
    pat1.embedsInto(dna2) shouldEqual true
    snap.embeddingsOf(pat1).size shouldEqual 3
    val Parsed.Success(pat2, _) = parser.rulePart.parse("DNA(up!2, down), DNA(up, down!2)")
    snap.embeddingsOf(pat2).size shouldEqual 3

    val Parsed.Success(pat3, _) = parser.rulePart.parse("DNA(up, down!1), DNA(up!1, down!2), DNA(up!2, down)")
    snap.embeddingsOf(pat3).size shouldEqual 2
  }

  }

  lazy val dnaGenerator: Gen[Pattern] = {
    for {
     length <- Gen.choose(0, 100)
     quantity <- Gen.choose(0, 5000)
   } yield generateDna(length, Pattern.empty)

  }

  def sumPatterns(patterns: Map[KappaModel.Pattern, Int]): Int = patterns.foldLeft(0){ case (acc, (pat, q))  => acc + q}


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
