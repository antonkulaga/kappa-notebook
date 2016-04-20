package org.denigma.kappa.notebook.parsers
import fastparse.all._

/**
  * Created by antonkulaga on 24/03/16.
  */
class KappaParser extends CommentLinksParser
{
  import org.denigma.kappa.model.KappaModel._

  protected val text = P(digit | letter)

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

  protected val eNumber = P( normalNumber ~ "E" ~ integer ).map{
    case (a, b) => a * Math.pow(10, b)
  }

  val number: P[Double] = P( normalNumber | eNumber )


  val textWithSymbols = P(digit | letter | CharIn("!@#$%^&*()_+=-.,/\\|?><`~ "))

  val label: P[String] = P("'"~textWithSymbols.rep(min = 1).! ~ "'")

  val labelOrNumber: P[Either[String, Double]] = P(label.map(l=>Left(l)) | number.map(n=>Right(n)))

  val linkLabel: P[String] = P("!" ~ text.rep(min = 1).!)

  val state: P[State] = P("~" ~ text.rep(min = 1).!).map(s=> State(s) )

  val side: P[Side] = P(text.rep(min = 1).! ~ state.rep ~ linkLabel.rep)
    .map{ case (name, states, links) => Side(name, states.toSet, links.toSet) }

  val agent: P[Agent] = P(text.rep(min = 1).! ~ "(" ~ side.? ~ (optSpaces ~ "," ~ optSpaces ~ side).rep ~ ")").map{
    case (name, sideOpt, sides2) => Agent(name,  sideOpt.map(List(_)).getOrElse(List.empty[Side]) ::: sides2.toList)
  }

  val agentDecl: P[Agent] = P(optSpaces ~ "%agent:" ~ spaces ~ agent)

  val rulePart: P[Pattern] = P(agent ~ (optSpaces ~ "," ~ optSpaces ~ agent).rep).map{
    case (ag, agents) => Pattern(ag::agents.toList)
  }

  val coeffs = P("@" ~ AnyChar.rep)

  val bothDirections = P("<->").map(v=>BothDirections)
  val left2right = P("->").map(v=>Left2Right)
  val right2left = P("<-").map(v=>Right2Left)

  val direction: P[Direction] = P(bothDirections | left2right | right2left)

  val rule = P("'" ~ textWithSymbols.rep(min = 1).! ~ "'" ~ spaces ~  rulePart ~ optSpaces ~ direction ~ optSpaces ~ rulePart ~ spaces ~ coeffs).map{
    case (name, left, BothDirections, right) => Rule(name, left, right, 0)
    case (name, left, Left2Right, right) => Rule(name, left, right, 0)
    case (name, left, Right2Left, right) => Rule(name, left, right, 0)
  }


  /*
  val rule = P("'" ~ text.! ~ "'" ~ rulePart ~ direction ~ rulePart ).map{
    case (name, left, BothDirections, right) => Rule(name, left, right, 0)
    case (name, left, Left2Right, right) => Rule(name, left, right, 0)
    case (name, left, Right2Left, right) => Rule(name, left, right, 0)
  }
  */


  //val leftSide =

}
