package org.denigma.kappa.notebook.parsers

import fastparse.all._
import org.denigma.controls.papers.TextLayerSelection
import org.denigma.controls.papers.TextLayerSelection.SimpleTextLayerSelection
import org.denigma.kappa.notebook.parsers.AST.IRI

object AST {
  trait ASTElement

  case class IRI(value: String) extends ASTElement
  {
    protected lazy val semicol = value.indexOf(":")

    lazy val namespace = value.substring(0, Math.max(semicol, 0))
    lazy val local = value.substring(Math.min(semicol+1, value.length))
  }

}

//TODO: merge with
case class PaperSelection(paper: IRI, page: Int, from_chunk: Int, to_chunk: Int, fromTokenOpt: Option[Int] = None, toTokenOpt: Option[Int] = None, comment: String = "") extends TextLayerSelection {

  override lazy val label: String = paper.value.replace("%20"," ") match {
    case prefixed if prefixed.startsWith(":") => prefixed.tail
    case iri => iri
  }

  override def fromChunk: String = from_chunk.toString

  override def toChunk: String = to_chunk.toString

  override def fromToken: String = fromTokenOpt.map(t =>t.toString).getOrElse("")

  override def toToken: String = toTokenOpt.map(t => t.toString).getOrElse("")
}

class PaperParser extends RDFParser with BasicParser {

  lazy val UINT_VALUE = P(DIGIT.rep(1).!).map(v => Integer.parseInt(v))

  lazy val comment = P( PNAME_NS ~ ("comment" | "has_comment") ~ spaces ~ STRING_LITERAL_QUOTE)

  lazy val page = P( PNAME_NS ~ ("on_page" | "page") ~ spaces ~ UINT_VALUE)

  lazy val paper = P(PNAME_NS ~ ("in_paper" | "paper") ~ spaces ~ IRI).map(AST.IRI)

  lazy val from_chunk = P(PNAME_NS ~ "from_chunk" ~ spaces ~ UINT_VALUE)

  lazy val to_chunk = P(PNAME_NS ~ "to_chunk" ~ spaces ~ UINT_VALUE)

  lazy val from_token = P(PNAME_NS ~ "from_token" ~ spaces ~ UINT_VALUE)

  lazy val to_token = P(PNAME_NS ~ "to_token" ~ spaces ~ UINT_VALUE)

  lazy val annotation: P[PaperSelection] = P(optSpaces ~ (comment ~ d).? ~ paper ~ d ~ page ~ d ~ from_chunk ~ d ~ to_chunk ~ (d ~ from_token).? ~ (d ~ to_token).? ~ optSpaces ~ ".".? ).map{
    case (commentOpt, pap, pg, fromChunk, toChunk, fromTokenOpt, toTokenOpt) =>
        PaperSelection(pap, pg, fromChunk, toChunk, fromTokenOpt, toTokenOpt)
  }

  //val page = P( optSpaces ~  (":on_page" | ":page") ~ spaces ~ CharIn('0' to '9').rep.!.map(v=>v.toInt) )
  // val paper = P( optSpaces ~  (":in_paper" | ":paper") ~ spaces ~ AnyChar.rep.! )


  //val annotation = P(paper ~ page ~)
}
