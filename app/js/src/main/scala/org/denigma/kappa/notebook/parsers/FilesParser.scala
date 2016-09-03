package org.denigma.kappa.notebook.parsers

import fastparse.all._
import org.denigma.kappa.notebook.views.figures.{Image, Video}

class FilesParser extends ExtRDFParser{
  lazy val lineNum = P(optSpaces ~PNAME_NS ~ ("on_line" | "line") ~ spaces ~ integer)

  lazy val onLine = P(optSpaces ~ (comment ~ d).?  ~ lineNum)

  lazy val source = P(optSpaces ~ (comment ~ d).? ~ PNAME_NS ~ ("in_source" | "source" | "in_code" | "code") ~ spaces ~ IRI_lite ~ (d ~ lineNum).?)
    .map{ case (com, a, b)=> (com, AST.IRI(a), b)}

  lazy val video = P( optSpaces ~ (comment ~ d).? ~ PNAME_NS ~ "video" ~ spaces ~ IRI_lite)
    .map{ case (com, v) =>
      val result = AST.IRI(v)
      Video(result.path, result.path, com.getOrElse(""))
    }
  lazy val image = P( optSpaces ~ (comment ~ d).? ~ PNAME_NS ~ "image" ~ spaces ~ IRI_lite)
    .map{ case (com , v)=>
      val result = AST.IRI(v)
      Image(result.path, result.path, com.getOrElse(""))
    }

}