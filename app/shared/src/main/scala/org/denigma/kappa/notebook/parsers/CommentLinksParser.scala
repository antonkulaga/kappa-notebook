package org.denigma.kappa.notebook.parsers

import fastparse.all._

trait BasicParser {
  protected val optSpaces = P(" ".rep)
  protected val spaces = P(" ".rep(min = 1))
  protected val digit = P(CharIn('0'to'9'))
  protected val letter = P(CharIn('A' to 'Z') | CharIn('a' to 'z'))

}

class CommentLinksParser extends BasicParser
{
  protected val propertyComment = P("#^")
  protected val commentSign = P( "#".rep(min = 1) ~ "^".?)
  protected val notComment = P( ! commentSign ).flatMap(v => AnyChar)
  protected val bracketsOrSpace = P("<" | ">" | " ")
  protected val notBracketsOrSpace = CharPred(ch => ch != '<' && ch != '>' && ch != ' ')
  protected val protocol = P( ("http" | "ftp" ) ~ "s".? ~ "://" )
  val comment = notComment.rep  ~ commentSign ~ AnyChar.rep.!
  val link: P[String] = P(optSpaces ~ "<".? ~ (protocol ~ notBracketsOrSpace.rep).! ~ ">".? ) //map(_.toString)
  val linkAfterComment: Parser[String] = P( notComment.rep  ~ commentSign ~ optSpaces ~ link )
  //val page = P( notComment.rep  ~ commentSign ~ optSpaces ~  ":on_page" ~ spaces ~ CharIn('0' to '9').rep.!.map(v=>v.toInt) )


}

class PaperParser extends BasicParser {
  val page = P( optSpaces ~  ":on_page" ~ spaces ~ CharIn('0' to '9').rep.!.map(v=>v.toInt) )
  val paper = P( optSpaces ~  ":in_paper" ~ spaces ~AnyChar.! )
}
