package org.denigma.kappa.notebook.parsers

import fastparse.all._
/*
class PaperCommentsParser extends CommentLinksParser {
  val propertyParser = P(propertyComment ~ " ".rep)
  //#^ :in_paper /resources/pdf/eptcs.pdf
  //#^ :on_page 1
}
*/

class CommentLinksParser
{
  protected val propertyComment = P("#^")
  protected val comment = P( "##" | propertyComment  | "#" )
  protected val notComment = P( ! comment ).flatMap(v => AnyChar)
  protected val bracketsOrSpace = P("<" | ">" | " ")
  protected val notBracketsOrSpace = CharPred(ch => ch != '<' && ch != '>' && ch != ' ')
  protected val protocol = P( ("http" | "ftp" ) ~ "s".? ~ "://" )
  val link: P[String] = P("<".? ~ (protocol ~ notBracketsOrSpace.rep).! ~ ">".? ) //map(_.toString)
  val linkAfterComment: Parser[String] = P( notComment.rep  ~ comment ~ " ".rep ~ link )
  val page = P( notComment.rep  ~ comment ~ " ".rep ~  ":on_page" ~ " ".rep ~ CharIn('0' to '9').rep.!.map(v=>v.toInt) )
}

/*case object FeaturesParser extends CommentsParser {
  def parse(str: String) = linkAfterComment.parse(str)
}*/

