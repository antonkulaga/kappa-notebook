package org.denigma.kappa

import fastparse.core.Parsed
import org.denigma.kappa.notebook.parsers.{CommentLinksParser, KappaParser, PaperParser}
import org.scalatest.{Inside, Matchers, WordSpec}

import scala.util.Success


class CommentsParserSuite extends WordSpec with Matchers with Inside  {

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
