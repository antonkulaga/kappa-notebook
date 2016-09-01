package org.denigma.kappa.notebook.parsers

import fastparse.all._

class FilesParser extends BasicParser with RDFParser{

  lazy val IRI_lite = P("<".? ~ ((IRIREF_CHAR | UCHAR).rep).! ~ ">".? | PrefixedName) //just to ease the restrictions

  lazy val onLine = P(optSpaces ~PNAME_NS ~ ("on_line" | "line") ~ spaces ~ integer)
  lazy val source = P(optSpaces ~PNAME_NS ~ ("in_source" | "source" | "in_code" | "code") ~ spaces ~ IRI_lite ~ (d ~ onLine).?).map{ case (a, b)=> (AST.IRI(a), b)}
  lazy val video = P( optSpaces ~PNAME_NS ~ "video" ~ spaces ~ IRI_lite).map(v=>AST.IRI(v))
  lazy val image = P( optSpaces ~ PNAME_NS ~ "image" ~ spaces ~ IRI_lite).map(v=>AST.IRI(v))

}