package org.denigma.kappa.notebook.parsers

import fastparse.all._

class FilesParser extends BasicParser with RDFParser{

  lazy val onLine = P(PNAME_NS ~ ("on_line" | "line") ~ spaces ~ integer)
  lazy val source = P(PNAME_NS ~ ("in_source" | "source" | "in_code" | "code") ~ spaces ~ IRI ~ (d ~ onLine).?)
  lazy val video = P( PNAME_NS ~ "video" ~ spaces ~ IRI).map(AST.IRI)
  lazy val image = P( PNAME_NS ~ "image" ~ spaces ~ IRI).map(AST.IRI)

}