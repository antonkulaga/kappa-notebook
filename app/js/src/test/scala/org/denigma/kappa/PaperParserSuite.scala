package org.denigma.kappa

import fastparse.core.Parsed
import org.denigma.kappa.notebook.parsers.{AST, PaperParser, PaperSelection}
import org.scalatest.{Inside, Matchers, WordSpec}

class PaperParserSuite extends WordSpec with Matchers with Inside  {


  "parse IRIs and prefixes" in {
    lazy val paper = ":paper"
    lazy val hello = "<http://helloworld>"
    lazy val page = ":page"
    val parser = new PaperParser
    inside(parser.IRI.parse(hello)) { case Parsed.Success(_, 19) =>}
    inside(parser.IRI.parse("http helloworld")) { case f: Parsed.Failure =>}
    inside(parser.IRIREF.parse(paper)) { case f: Parsed.Failure =>}
    inside(parser.IRI.parse(paper)) { case Parsed.Success(_, 6)=>}
    inside(parser.IRI.parse(page)) { case Parsed.Success(_, 5)=>}

  }

  "parse pages, chunks and tokens" in {

    val parser = new PaperParser
    inside(parser.page.parse(":page 20")){
      case Parsed.Success(20, _)=>
    }
    inside(parser.page.parse(":on_page 1")){
      case Parsed.Success(1, _)=>
    }

    inside(parser.page.parse("kappa:on_page 4")){
      case Parsed.Success(4, _)=>
    }

    inside(parser.page.parse("on_page 20")){
      case f: Parsed.Failure=>
    }

    inside(parser.page.parse(":from_token 20")){
      case f: Parsed.Failure=>
    }

    inside(parser.page.parse(":page hello")){
      case f: Parsed.Failure=>
    }

    inside(parser.from_chunk.parse(":from_chunk 20")){
      case Parsed.Success(20, _)=>
    }

    inside(parser.to_chunk.parse(":to_chunk 20")){
      case Parsed.Success(20, _)=>
    }

    inside(parser.from_token.parse(":from_token 1")){
      case Parsed.Success(1, _)=>
    }


    inside(parser.to_token.parse(":to_token 1")){
      case Parsed.Success(1, _)=>
    }

    inside(parser.to_token.parse(":to_token hello")){
      case f: Parsed.Failure=>
    }
  }

  "parse papers" in {
    val parser = new PaperParser
    inside(parser.paper.parse(":in_paper <http://helloworld/paper.pdf>")){
      case Parsed.Success(AST.IRI("http://helloworld/paper.pdf"), _) =>
    }
    inside(parser.paper.parse(":in_paper files:paper.pdf")){
      case Parsed.Success(iri @ AST.IRI("files:paper.pdf"), _) if iri.namespace =="files" && iri.local =="paper.pdf" =>
    }

  }

  "parse whole line" in {
    val parser = new PaperParser
    inside(parser.annotation.parse(":in_paper <http://helloworld/paper.pdf>; :on_page 15 ; :from_chunk 11 ; :to_chunk 30 ; :from_token 1 ; :to_token 2")){
      case Parsed.Success(PaperSelection(AST.IRI("http://helloworld/paper.pdf"), 15, 11, 30, Some(1), Some(2)), _) =>
    }
    inside(parser.annotation.parse(":in_paper :hello; :page 10 ; :from_chunk 0 ; :to_chunk 3 ; :from_token 1 ; :to_token 2")){
      case Parsed.Success(PaperSelection(AST.IRI(":hello"), 10, 0, 3, Some(1), Some(2)), _) =>
    }
  }

}
