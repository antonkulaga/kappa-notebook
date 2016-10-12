package org.denigma.kappa

import fastparse.core.Parsed
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.parsers.RDFParser
import org.scalatest.{Inside, Matchers, WordSpec}

class RDFParserSuite extends WordSpec with Matchers with Inside  {

  "RDF Parser" should {
    "parse IRI REF " in {
      inside(RDFParser.IRIREF.parse("<http://example/s>")) {
        case Parsed.Success("http://example/s", _) =>
      }
      inside(RDFParser.IRI.parse("<http://example/s>")) {
        case Parsed.Success("http://example/s", _) =>
      }
      inside(RDFParser.IRI.parse("<http://localhost:1234/files/repressilator/Kappa%20in%20Synthetic%20Biology.pdf>")) {
        case Parsed.Success("http://localhost:1234/files/repressilator/Kappa%20in%20Synthetic%20Biology.pdf", _) =>
      }
    }

    "parse prefixed name" in {
        inside(RDFParser.PrefixedName.parse(":on_page")) {
          case Parsed.Success(":on_page", _) =>
        }
        inside(RDFParser.IRI.parse(":on_page")) {
          case Parsed.Success(":on_page", _) =>
        }
        inside(RDFParser.PrefixedName.parse(":repressilator/Kappa%20in%20Synthetic%20Biology.pdf")) {
          case Parsed.Success(":repressilator/Kappa%20in%20Synthetic%20Biology.pdf", _) =>
        }
      }

    "parse string literal" in {
      inside(RDFParser.STRING_LITERAL_QUOTE.parse("\"hello world\"")) {
        case Parsed.Success("hello world", _) =>
      }
    }
  }
}
