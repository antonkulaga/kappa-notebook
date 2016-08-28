package org.denigma.kappa

import fastparse.core.Parsed
import org.denigma.kappa.notebook.parsers.{CommentLinksParser, FilesParser, KappaParser}
import org.scalatest.{Inside, Matchers, WordSpec}

class  FilesParserSuite extends WordSpec with Matchers with Inside  {
  "Source parser" should {
    "parse sources" in {
      val parser = new FilesParser
      val linenum = ":on_line 150"
      inside(parser.onLine.parse(linenum)) {
        case Parsed.Success(150, _) =>
      }
      val wrongLine = ":in_source"
      inside(parser.source.parse(wrongLine)){
        case f: Parsed.Failure =>
      }
      val linePrefixed = ":in_source :DNA_REPAIR/figure.jpg"
      inside(parser.source.parse(linePrefixed))
      {
        case Parsed.Success((":DNA_REPAIR/figure.jpg", None), _) =>
      }
      val lineURL = ":in_source <http://helloworld.com>"
      inside(parser.source.parse(lineURL))
      {
        case Parsed.Success(("http://helloworld.com", None), _) =>
      }

      val place = s":in_source :DNA_REPAIR/figure.jpg ; $linenum"
      inside(parser.source.parse(place)){
        case Parsed.Success((":DNA_REPAIR/figure.jpg", Some(150)), _) =>
      }
    }
    /*
    "parse figures" in {
      val parser = new FilesParser
    }
    */
  }


}
