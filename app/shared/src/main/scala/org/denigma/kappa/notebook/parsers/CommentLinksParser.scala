package org.denigma.kappa.notebook.parsers

import fastparse.all._

class CommentLinksParser
{
  protected val optSpaces = P(" ".rep)
  protected val spaces = P(" ".rep(min = 1))
  protected val digit = P(CharIn('0'to'9'))
  protected val letter = P(CharIn('A' to 'Z') | CharIn('a' to 'z'))

  protected val propertyComment = P("#^")
  protected val comment = P( "##" | propertyComment  | "#" )
  protected val notComment = P( ! comment ).flatMap(v => AnyChar)
  protected val bracketsOrSpace = P("<" | ">" | " ")
  protected val notBracketsOrSpace = CharPred(ch => ch != '<' && ch != '>' && ch != ' ')
  protected val protocol = P( ("http" | "ftp" ) ~ "s".? ~ "://" )
  val link: P[String] = P("<".? ~ (protocol ~ notBracketsOrSpace.rep).! ~ ">".? ) //map(_.toString)
  val linkAfterComment: Parser[String] = P( notComment.rep  ~ comment ~ optSpaces ~ link )
  val page = P( notComment.rep  ~ comment ~ optSpaces ~  ":on_page" ~ spaces ~ CharIn('0' to '9').rep.!.map(v=>v.toInt) )
}

/*case object FeaturesParser extends CommentsParser {
  def parse(str: String) = linkAfterComment.parse(str)
}*/

