package org.denigma.kappa.notebook.parsers

import fastparse.all._

trait BasicParser {
  protected val optSpaces = P(CharIn(" \t").rep(min = 0))
  protected val spaces = P(CharIn(" \t").rep(min = 1))
  protected val digit = P(CharIn('0'to'9'))
  protected val letter = P(CharIn('A' to 'Z') | CharIn('a' to 'z'))
  protected val d = P(optSpaces ~ CharIn(";\n") ~ optSpaces)

  protected val integer: P[Int] = P(
    "-".!.? ~ digit.rep(min = 1).!).map{
    case (None, str) => str.toInt
    case (Some(_), str) => - str.toInt
  }


  protected val normalNumber = P(
    integer ~ ("."~ integer).?
  ).map{
    case (i, None) => i.toDouble
    case (i, Some(o)) => (i + "." + o).toDouble
  }

  protected val eNumber = P( normalNumber ~ CharIn("Ee") ~ integer ).map{
    case (a, b) => a * Math.pow(10, b)
  }

  val number: P[Double] = P( eNumber | normalNumber )
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


}