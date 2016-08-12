package org.denigma.kappa

import fastparse.core.Parsed
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.parsers.{CommentLinksParser, KappaParser, RDFParser}
import org.scalatest.{Inside, Matchers, WordSpec}

class RDFParserSuite extends WordSpec with Matchers with Inside  {

  "parse " in {

    inside(RDFParser.IRIREF.parse("<http://example/s>")){
      case Parsed.Success(_, _) =>
    }

    inside(RDFParser.IRI.parse("<http://example/s>")){
      case Parsed.Success(_, _) =>
    }

  }
}
