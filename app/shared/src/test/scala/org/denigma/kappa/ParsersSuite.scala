package org.denigma.kappa

import fastparse.core.Parsed
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.parsers.{CommentLinksParser, KappaParser}
import org.scalatest.{Inside, Matchers, WordSpec}

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

    "parse rules" in {
      import KappaModel._
      val parser = new KappaParser
      val wrong = "'a.b'qwe A(x),B(x) <-> A(x!1),B(x!1) @ 'on_rate','off_rate' #A binds B "
      inside(parser.rule.parse(wrong)) {
        case failure: Parsed.Failure =>
      }
      val right = "'a.b' A(x),B(x) <-> A(x!1),B(x!1) @ 'on_rate','off_rate'"
      inside(parser.rule.parse(right)) {
        case res @ Parsed.Success(v, index: Int) if v.left.agents =>
      }

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
      inside(parser.number.parse("-10")) { case Parsed.Success(10, index: Int)=>  }
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