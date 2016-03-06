package org.denigma.kappa.notebook.parsers

import fastparse.all._

class CommentLinksParser
{
  val comment = P( "##" | "#^" | "#" )
  val notComment = P( ! comment ).flatMap(v => AnyChar)
  val bracketsOrSpace = P("<" | ">" | " ")
  val notBracketsOrSpace = CharPred(ch => ch != '<' && ch != '>' && ch != ' ')
  val protocol = P( ("http" | "ftp" ) ~ "s".? ~ "://" )
  val link: P[String] = P("<".? ~ (protocol ~ notBracketsOrSpace.rep).! ~ ">".? ) //map(_.toString)
  val linkAfterComment: Parser[String] = P( notComment.rep  ~ comment ~ " ".rep ~ link )
}

/*case object FeaturesParser extends CommentsParser {
  def parse(str: String) = linkAfterComment.parse(str)
}*/

