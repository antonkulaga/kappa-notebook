package org.denigma.kappa

import fastparse.core.Parsed
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.parsers.{CommentLinksParser, KappaParser}
import org.scalatest.{Inside, Matchers, WordSpec}

import scala.collection.immutable.SortedSet

/**
  * Created by antonkulaga on 06/03/16.
  */
class ParsersSuite extends WordSpec with Matchers with Inside  {

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
        case res @ Parsed.Success(v, index: Int) if v == Agent("A", List(Side("x", Set(), Set()), Side("c", Set(State("u")), Set("1"))))=>
      }

      val A = "%agent: A(x,c) # Declaration of agent A"

      inside(parser.agentDecl.parse(A)) {
        case res @ Parsed.Success(v, index: Int)  if v==KappaModel.Agent("A", List(Side("x"), Side("c"))) =>
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
        case res @ Parsed.Success(v, index: Int) =>
      }

      val withLink = "'a binds to b' A(x),B(x),C(x!_) <-> A(x!1),B(x!1),C(x!_) @ 'on_rate','off_rate'"
      inside(parser.rule.parse(withLink)) {
        case res @ Parsed.Success(v, index: Int) if v.name == "a binds to b"
          && v.left.agents.length ==3
          && v.right.agents.length ==3
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
        case res @ Parsed.Success(v, index: Int) =>
      }
      val r1 = parser.rule.parse(ac1).get.value
      val r2 = parser.rule.parse(ac2).get.value
      (r1.left.agents == r2.left.agents) shouldEqual false
      val ag1 = Agent("A",List(Side("c",Set(),Set("1"))))
      val ag2 = Agent("A",List(Side("x",Set(),Set())))
      (ag1 == ag2) shouldEqual false
      (SortedSet(ag1) == SortedSet(ag2)) shouldEqual false
      (Agent("A",List(Side("c",Set(),Set("1")))) == ag1) shouldEqual true
      (SortedSet(ag1) == SortedSet(Agent("A",List(Side("c",Set(),Set("1")))))) shouldEqual true

      //TreeSet(Agent(A,List(Side(x,Set(),Set()), Side(c,Set(),Set()))), Agent(C,List(Side(x1,Set(State(p)),Set()), Side(x2,Set(State(u)),Set()))))
    }

    "parse comments" in {

      val parser = new CommentLinksParser
      inside(parser.linkAfterComment.parse("'a.b' A(x),B(x) <-> A(x!1),B(x!1) @ 'on_rate','off_rate' #A binds B")) {
        case failure: Parsed.Failure =>
      }

      val comment = "#^ hello world"

      inside(parser.linkAfterComment.parse(comment)) {
        case failure: Parsed.Failure =>
      }

      val linkAfterComment = "#^ http://hello.world"

      inside(parser.linkAfterComment.parse(linkAfterComment)) {
        case Parsed.Success(value: String, index: Int) if value=="http://hello.world" =>
          //println("parsed comment = "+value)
      }

    }
    /*
    "parse PDF comments" in {
      val paper = "#^ :in_paper /resources/pdf/eptcs.pdf"
      val page = "#^ :on_page 1"
    }
    */
    "parse numbers" in {

      val parser = new KappaParser
      inside(parser.number.parse("10")) { case Parsed.Success(10, index: Int)=>  }
      inside(parser.number.parse("-10")) { case Parsed.Success(-10, index: Int)=>  }
      inside(parser.number.parse("10.1234")) { case Parsed.Success(10.1234, index: Int)=>  }
      inside(parser.number.parse("10E2")) { case Parsed.Success(10E2, index: Int)=>  }
      inside(parser.number.parse("10.9E3")) { case Parsed.Success(10.9E3, index: Int)=>  }
    }
  }
}

trait TestModel {

  val data =
    """
      |####### TEMPLATE MODEL AS DESCRIBED IN THE MANUAL#############
      |
      |#### Signatures
      |%agent: A(x,c) # Declaration of agent A
      |%agent: B(x) # Declaration of B
      |%agent: C(x1~u~p,x2~u~p) # Declaration of C with 2 modifiable sites
      |
      |#### Rules
      |'a.b' A(x),B(x) <-> A(x!1),B(x!1) @ 'on_rate','off_rate' #A binds B
      |'ab.c' A(x!_,c),C(x1~u) ->A(x!_,c!2),C(x1~u!2) @ 'on_rate' #AB binds C
      |'mod x1' C(x1~u!1),A(c!1) ->C(x1~p),A(c) @ 'mod_rate' #AB modifies x1
      |'a.c' A(x,c),C(x1~p,x2~u) -> A(x,c!1),C(x1~p,x2~u!1) @ 'on_rate' #A binds C on x2
      |'mod x2' A(x,c!1),C(x1~p,x2~u!1) -> A(x,c),C(x1~p,x2~p) @ 'mod_rate' #A modifies x2
      |
      |#### Variables
      |%var: 'on_rate' 1.0E-4 # per molecule per second
      |%var: 'off_rate' 0.1 # per second
      |%var: 'mod_rate' 1 # per second
      |%obs: 'AB' A(x!x.B)
      |%obs: 'Cuu' C(x1~u?,x2~u?)
      |%obs: 'Cpu' C(x1~p?,x2~u?)
      |%obs: 'Cpp' C(x1~p?,x2~p?)
      |
      |
      |#### Initial conditions
      |%init: 1000 A(),B()
      |%init: 10000 C()
      |
      |%mod: [true] do $FLUX "flux.html" [true]
      |%mod: [T]>20 do $FLUX "flux.html" [false]
      |%def: "relativeFluxMaps" "true"
    """.stripMargin

}