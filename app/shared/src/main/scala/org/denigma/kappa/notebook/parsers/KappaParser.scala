package org.denigma.kappa.notebook.parsers
import fastparse.all._

/**
  * Created by antonkulaga on 24/03/16.
  */
class KappaParser extends CommentLinksParser
{
  import org.denigma.kappa.model.KappaModel._
  val text = P(digit | letter)
  val state: P[State] = P("~" ~ text.rep(min = 1).!).map(s=> State(s) )
  val side: P[Side] = P(text.rep(min = 1).! ~ state.rep).map{ case (name, states) => Side(name, states.toSet) }

  val agent: P[Agent] = P(text.rep(min = 1).! ~ "(" ~ side.? ~ (optSpaces ~ "," ~ optSpaces ~side).rep ~ ")").map{
    case (name, sideOpt, sides2) => Agent(name,  sides2.toList)
  }

  val agentDecl: P[Agent] = P(optSpaces ~ "%agent:" ~ spaces ~ agent)

  val rulePart: P[Pattern] = P(agent ~ (optSpaces ~ "," ~ optSpaces ~ agent).rep).map{
    case (ag, agents) => Pattern(agents.toSet + ag)
  }

  val bothDirections = P("<-").map(v=>Right2Left)
  val left2right = P("->").map(v=>Left2Right)
  val right2left = P("<-").map(v=>Right2Left)

  val direction: P[Direction] = P(optSpaces ~ bothDirections | left2right | right2left ~ optSpaces)

  /*
  val rule = P("'" ~ text.! ~ "'" ~ rulePart ~ direction ~ rulePart ).map{
    case (name, left, BothDirections, right) => Rule(name, left, right, 0)
    case (name, left, Left2Right, right) => Rule(name, left, right, 0)
    case (name, left, Right2Left, right) => Rule(name, left, right, 0)
  }
  */


  //val leftSide =

}
