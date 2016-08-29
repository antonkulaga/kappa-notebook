package org.denigma.kappa.notebook.parsers

import fastparse.all._

class FilesParser extends BasicParser with RDFParser{

  lazy val onLine = P(optSpaces ~PNAME_NS ~ ("on_line" | "line") ~ spaces ~ integer)
  lazy val source = P(optSpaces ~PNAME_NS ~ ("in_source" | "source" | "in_code" | "code") ~ spaces ~ IRI ~ (d ~ onLine).?).map{ case (a, b)=> (AST.IRI(a), b)}
  lazy val video = P( optSpaces ~PNAME_NS ~ "video" ~ spaces ~ IRI).map(v=>AST.IRI(v))
  lazy val image = P( optSpaces ~ PNAME_NS ~ "image" ~ spaces ~ IRI).map(v=>AST.IRI(v))

}