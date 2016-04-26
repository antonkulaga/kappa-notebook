package org.denigma.kappa

import fastparse.core.Parsed
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.parsers.{CommentLinksParser, KappaParser}
import org.scalatest.{Inside, Matchers, WordSpec}

import scala.collection.immutable.SortedSet

/**
  * Created by antonkulaga on 06/03/16.
  */
class KappaParsersSuite extends WordSpec with Matchers with Inside  {

  "Kappa parser" should {

    "parse agents" in {
      import KappaModel._
      val parser = new KappaParser
      inside(parser.agent.parse("%wrongagent: A(x,c) # Declaration of agent A")) {
        case failure: Parsed.Failure =>
      }

      inside(parser.agent.parse("'a.b' A(x),B(x) <-> A(x!1),B(x!1) @ 'on_rate','off_rate' #A binds B")) {
        case failure: Parsed.Failure =>
      }

      inside(parser.agent.parse("A(x,c~u!1)")) {
        case res@Parsed.Success(v, index: Int) if v == Agent("A", List(Side("x", Set(), Set()), Side("c", Set(State("u")), Set("1")))) =>
      }

      val A = "%agent: A(x,c) # Declaration of agent A"

      inside(parser.agentDecl.parse(A)) {
        case res@Parsed.Success(v, index: Int) if v == KappaModel.Agent("A", List(Side("x"), Side("c"))) =>
      }

    }

    "parse tokens" in {
      import KappaModel._
      val parser = new KappaParser
      val wrong = "token: atp"
      inside(parser.tokenDeclaration.parse(wrong)) {
        case failure: Parsed.Failure =>
      }


      val right = "%token: atp"
      inside(parser.tokenDeclaration.parse(right)) {
        case Parsed.Success("atp", index) =>

      }

    }

    "parse rules" in {
      import KappaModel._
      val parser = new KappaParser
      val wrong = "'a.b'qwe A(x),B(x) <-> A(x!1),B(x!1) @ 'on_rate','off_rate' #A binds B "
      inside(parser.rule.parse(wrong)) {
        case failure: Parsed.Failure =>
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
          && v.right.agents.tail.head == Agent("B", List(Side("x", Set(), Set("1"))))
        =>
      }
      val withoutName = "A(x),B(x),C(x!_) <-> A(x!1),B(x!1),C(x!_) @ 'on_rate','off_rate'"
      inside(parser.rule.parse(withLink)) {
        case res@Parsed.Success(v, index: Int) if v.name == "a binds to b"
          && v.left.agents.length == 3
          && v.right.agents.length == 3
          && v.right.agents.tail.head == Agent("B", List(Side("x", Set(), Set("1"))))
        =>
      }
    }

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
  }

}