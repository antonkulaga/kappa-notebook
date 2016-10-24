package org.denigma.kappa

import fastparse.core.Parsed
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.parsers.KappaParser
import org.scalatest.{Inside, Matchers, WordSpec}

import scala.collection.immutable._

/**
  * Created by antonkulaga on 06/03/16.
  */
class KappaParsersSuite extends WordSpec with Matchers with Inside  {

  "Kappa parser" should {

    "parse agents" in {
      import KappaModel._
      val parser = new KappaParser
      inside(parser.agent.parse("%wrongagent: A(x,c) # Declaration of agent A")) {
        case failure: Parsed.Failure[_, _] =>
      }

      inside(parser.agent.parse("'a.b' A(x),B(x) <-> A(x!1),B(x!1) @ 'on_rate','off_rate' #A binds B")) {
        case failure: Parsed.Failure[_, _] =>
      }

      inside(parser.agent.parse("A(x,c~u!1)")) {
        case res@Parsed.Success(v, index: Int) if v == Agent("A", Set(Site("x", Set(), Set()), Site("c", Set(State("u")), Set("1")))) =>
      }

      val A = "%agent: A(x,c) # Declaration of agent A"

      inside(parser.agentDecl.parse(A)) {
        case res@Parsed.Success(v, index: Int) if v == KappaModel.Agent("A", Set(Site("x"), Site("c"))) =>
      }

    }

    "parse tokens" in {
      import KappaModel._
      val parser = new KappaParser
      val wrong = "token: atp"
      inside(parser.tokenDeclaration.parse(wrong)) {
        case failure: Parsed.Failure[_, _] =>
      }
      val right = "%token: atp"
      inside(parser.tokenDeclaration.parse(right)) {
        case Parsed.Success("atp", index) =>

      }
    }

    "parse one sided rule" in {
      import KappaModel._
      val parser = new KappaParser
      val wrong = "'a.b'qwe A(x),B(x) -> A(x!1),B(x!1) @ 'on_rate','off_rate' #A binds B "
      inside(parser.rule.parse(wrong)) {
        case failure: Parsed.Failure[_, _] =>
      }
      val right = "'a.b' A(x),B(x) -> A(x!1),B(x!1) @ 'on_rate'"
      inside(parser.rule.parse(right)) {
        case res@Parsed.Success(v, index: Int) =>
      }

      val pTetLeft = Agent("pTet", Set(Site("binding", Set.empty, Set("1"))))
      val tetRLeft = Agent("TetR", Set(Site("dna", Set.empty, Set("1"))))
      val pTetRight = Agent("pTet", Set(Site("binding", Set.empty, Set.empty)))
      val leftPattern =  Pattern(List(pTetLeft, tetRLeft))
      val rightPattern = Pattern(List(pTetRight))

      val rule = parser.mergeLine(
        """
          |'tetR.degradation2' pTet(binding!1),TetR(dna!1) ->  pTet(binding) @ 'degrad2'
        """.stripMargin)
      inside(parser.rule.parse(rule)) {
        case Parsed.Success(v: Rule, _) if
        v.left == leftPattern &&
          v.right == rightPattern =>
      }
    }

    "parse two sided rules" in {
      import KappaModel._
      val parser = new KappaParser
      val wrong = "'a.b'qwe A(x),B(x) <-> A(x!1),B(x!1) @ 'on_rate','off_rate' #A binds B "
      inside(parser.rule.parse(wrong)) {
        case failure: Parsed.Failure[_, _] =>
      }
      val right = "'a.b' A(x),B(x) <-> A(x!1),B(x!1) @ 'on_rate','off_rate'"
      inside(parser.rule.parse(right)) {
        case res@Parsed.Success(v, index: Int) =>
      }

      val withLink = "'a binds to b' A(x),B(x),C(x!_) <-> A(x!1),B(x!1),C(x!_) @ 'on_rate','off_rate'"
      inside(parser.rule.parse(withLink)) {
        case res@Parsed.Success(v, index: Int) if v.name == "a binds to b"
          && v.left.agents.length == 3
          && v.right.agents.length == 3
          && v.right.agents.tail.head == Agent("B", Set(Site("x", Set(), Set("1"))), position = 1)
        =>
      }
      val withoutName = "A(x),B(x),C(x!_) <-> A(x!1),B(x!1),C(x!_) @ 'on_rate','off_rate'"
      inside(parser.rule.parse(withoutName)) {
        case res@Parsed.Success(v, index: Int) if v.name.contains("<->")
          && v.left.agents.length == 3
          && v.right.agents.length == 3
          && v.right.agents.tail.head == Agent("B", Set(Site("x", Set(), Set("1"))), position = 1)
        =>
      }

    }

    "parse creation and degradation" in {
      val parser = new KappaParser
      val degradation = "'tetR.degradation' TetR(dna) ->  @ 'degrad1'"
      inside(parser.rule.parse(degradation)) {
        case res@Parsed.Success(v, index: Int) if v.name == "tetR.degradation"
          && v.left.agents.length == 1
          && v.right.agents.isEmpty
        =>
      }
      val creation = "'synthesis' -> LacI() @ 'synth'"
      inside(parser.rule.parse(creation)) {
        case res@Parsed.Success(v, index: Int) if v.name == "synthesis"
          && v.left.agents.isEmpty
          && v.right.agents.length == 1
        =>
      }
    }

    "observable parsing" in {
      val ob = "%obs: 'TetR' TetR()"
      val parser = new KappaParser
      inside(parser.observable.parse(ob)) {
        case res@Parsed.Success(v, index: Int) if v.name == "TetR"
          && v.pattern.agents.length == 1
        =>
      }
    }

    "parse init conditions (partial)" in {
      val init = "%init: 100 pLac(binding),pTet(binding),pL(binding)"
      val parser = new KappaParser
      inside(parser.init.parse(init)) {
        case res@Parsed.Success(v, index: Int) if v.pattern.agents.length == 3
        =>
      }
    }

    "parsing agents with the same name" in {
      val degradation = "'tetR.degradation' TetR(dna),TetR(dna~hello),TetR(dna) ->  @ 'degrad1'"
      val parser = new KappaParser
      inside(parser.rule.parse(degradation)) {
        case res@Parsed.Success(v, index: Int) if v.name == "tetR.degradation"
          && v.left.agents.length == 3
          && v.right.agents.isEmpty
        =>
      }
    }

    "parsing complex agents" in {
      val parser = new KappaParser
      import KappaModel._
      val agent = parser.mergeLine("%agent: RNA(downstream,upstream,type~BBaB0034~BBaC0012~BBaC0040~BBaC0051~BBaR0010~BBaR0040~BBaR0051,binding)")
      lazy val states = Set(
        Site("downstream"), Site("upstream"), Site("binding"),
        Site("type", Set(State("BBaB0034"), State("BBaC0012"), State("BBaC0040"),
          State("BBaC0051"), State("BBaR0010"), State("BBaR0040"), State("BBaR0051"))) )
      val rightAgent = KappaModel.Agent("RNA", states )
      inside(parser.agentDecl.parse(agent)) {
        case Parsed.Success(v: KappaModel.Agent , _) if v ==rightAgent  =>
      }
    }

    "parsing agents with comments" in {
      val parser = new KappaParser
      import KappaModel._
      val line = """%agent: DNA( #double stranded DNA about kilobase long \
      chr~closed~opened, #state of chromatin \
        Base~mC~C~T~U~x, rd, #base of DNA, "x" means NO base\
      init~mC~C, \
      status~ok~dom~hole\
      )"""


    }

    "parsing complex rules" in {
      import KappaModel._
      val dnaLeft1 = Agent("DNA", Set(Site("binding"), Site("type", Set(State("BBaR0010p3"))), Site("upstream", Set.empty, Set("2"))), position = 0)
      val lacILeft = Agent("LacI", Set(Site("dna"), Site("lactose")), position = 1)
      val dnaLeft2 = Agent("DNA", Set(
        Site("downstream", Set.empty, Set("2")), Site("binding"), Site("type", Set(State("BBaR0010p2")))
      ), position = 2)
      val dnaRight1 = Agent("DNA", Set(Site("binding"), Site("type", Set(State("BBaR0010p3"))), Site("upstream", Set.empty, Set("3"))), position = 0)
      val lacIRight = Agent("LacI", Set(Site("dna", Set.empty, Set("1")), Site("lactose")), position = 1)
      val dnaRight2 = Agent("DNA", Set(
        Site("downstream", Set.empty, Set("3")), Site("binding", Set.empty, Set("1")), Site("type", Set(State("BBaR0010p2")))
      ), position = 2)
      val parser = new KappaParser
      val line = parser.mergeLine("""
                   |'LacI binding to R0010p2 (no LacI)' \
                   |	DNA(binding,type~BBaR0010p3,upstream!2), LacI(dna,lactose), DNA(downstream!2,binding,type~BBaR0010p2) -> \
                   |	DNA(binding,type~BBaR0010p3,upstream!3), LacI(dna!1,lactose), DNA(downstream!3,binding!1,type~BBaR0010p2) @ 'transcription factor binding rate'
                 """.stripMargin)
      val ruleResult = parser.rule.parse(line)
      inside(ruleResult) {
        case Parsed.Success(r, _) if r.left == Pattern(List(dnaLeft1, lacILeft, dnaLeft2)) && r.right == Pattern(List(dnaRight1, lacIRight, dnaRight2))=>
      }
      val rule = ruleResult.get.value
      rule.same.length shouldEqual 3
      rule.same.head._1 shouldEqual dnaLeft1
      rule.same.head._2 shouldEqual dnaRight1
      rule.same.tail.head._1 shouldEqual lacILeft
      rule.same.tail.head._2 shouldEqual lacIRight
      rule.left.links shouldEqual Set( ("upstream", 0, "downstream", 2))
      rule.right.links shouldEqual Set( ("upstream", 0, "downstream", 2), ("dna", 1, "binding", 2 ))
      rule.unchangedLinks shouldEqual Set( ("upstream", 0, "downstream", 2))
      rule.removedLinks.size shouldEqual 0
      rule.addedLinks.size shouldEqual 1
    }

    /*
    "parse links" in {
      import KappaModel._
      val parser = new KappaParser
      val text = parser.mergeLine("""
                                    |'LacI binding to R0010p2 (no LacI)' \
                                    |	DNA(binding,type~BBaR0010p3,upstream!2), LacI(dna,lactose), DNA(downstream!2,binding,type~BBaR0010p2) -> \
                                    |	DNA(binding,type~BBaR0010p3,upstream!3), LacI(dna!1,lactose), DNA(downstream!3,binding!1,type~BBaR0010p2) @ 'transcription factor binding rate'
                                  """.stripMargin)
      val res: Rule = parser.rule.parse(text).get.value
      val links1 = res.left.pairLinksIndexed
      links1.length.shouldEqual(1)
      inside(links1.head) {
        case (_, (0, 2)) => //println("LINKS1 HEAD + "+links1.head)
      }
      val links2 = res.right.pairLinksIndexed
      links2.length.shouldEqual(2)
      inside(links2){
        case (_,(0, 2))::(_, (1, 2))::Nil =>
          //println("LINKS 2 = " + links2)
      }
    }
*/

    /*
    "differentiate agents in rules" in {
      //'ab.c' A(x!_,c),C(x1~u) ->A(x!_,c!2),C(x1~u!2) @ 'on_rate' #AB binds C
      import KappaModel._
      val parser = new KappaParser
      val ac1 = "'mod x1' C(x1~u!1),A(c!1) ->C(x1~p),A(c) @ 'mod_rate' #AB modifies x1"
      val ac2 = "'a.c' A(x,c),C(x1~p,x2~u) -> A(x,c!1),C(x1~p,x2~u!1) @ 'on_rate' #A binds C on x2"
      inside(parser.rule.parse(ac1)) {
        case res@Parsed.Success(v, index: Int) =>
      }
      val r1 = parser.rule.parse(ac1).get.value
      val r2 = parser.rule.parse(ac2).get.value
      (r1.left.agents == r2.left.agents) shouldEqual false
      val ag1 = Agent("A", List(Side("c", Set(), Set("1"))))
      val ag2 = Agent("A", List(Side("x", Set(), Set())))
      (ag1 == ag2) shouldEqual false
      (SortedSet(ag1) == SortedSet(ag2)) shouldEqual false
      (Agent("A", List(Side("c", Set(), Set("1")))) == ag1) shouldEqual true
      (SortedSet(ag1) == SortedSet(Agent("A", List(Side("c", Set(), Set("1")))))) shouldEqual true

      //TreeSet(Agent(A,List(Side(x,Set(),Set()), Side(c,Set(),Set()))), Agent(C,List(Side(x1,Set(State(p)),Set()), Side(x2,Set(State(u)),Set()))))
    }
    */
  }

}