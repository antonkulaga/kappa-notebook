package org.denigma.kappa.notebook.parsers
import fastparse.all._
import org.denigma.kappa.messages.WebSimMessages.Observable

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

  protected val name = P(
    (digit | letter | CharIn("_+-")).rep(min = 1).!
  )


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

  val textWithSymbols = P(digit | letter | CharIn("_!@$%^&*()_+=-.,/|?><`~{}[]~"))

  val tokenDeclaration = P("%token:"~optSpaces~name)

  val label: P[String] = P("'"~(textWithSymbols | " ").rep(min = 1).! ~ "'")

  val labelOrNumber: P[Either[String, Double]] = P(label.map(l=>Left(l)) | number.map(n=>Right(n)))

  val linkLabel: P[String] = P( ("!" ~ text.rep(min = 1).!) | "!_".! | "?".!)

  val state: P[State] = P("~" ~ name).map(s=> State(s) )

  val side: P[Site] = P(name ~ state.rep ~ linkLabel.rep)
    .map{ case (n, states, links) => Site(n, states.toSet, links.toSet) }

  val agent: P[Agent] = P(name ~ "(" ~ side.? ~ (optSpaces ~ "," ~ optSpaces ~ side).rep ~ ")").map{
    case (n, sideOpt, sides2) => Agent(n,  sideOpt.map(s => sides2.toSet + s ).getOrElse(sides2.toSet))
  }

  val agentDecl: P[Agent] = P(optSpaces ~ "%agent:" ~ optSpaces ~ agent)

  val rulePart: P[Pattern] = P(agent ~ (optSpaces ~ "," ~ optSpaces ~ agent).rep).map{
    case (ag, agents) =>
      val ags = ag::agents.toList
      //val dist: List[Agent] = ags.distinct
      //val dupl: List[Agent] = ags.diff(dist)//make agents unique
      Pattern(ags)
  }

  val coeffs: P[(Either[String, Double], Option[Either[String, Double]])] = P("@" ~optSpaces ~ labelOrNumber ~ (optSpaces ~ ","~ optSpaces ~labelOrNumber).?)

  val bothDirections = P("<->").map(v=>BothDirections)

  val left2right = P("->").map(v=>Left2Right)

  val right2left = P("<-").map(v=>Right2Left)

  val direction: P[Direction] = P(bothDirections | left2right | right2left)

  val rule = P(optSpaces ~ label.? ~ optSpaces ~  rulePart.? ~ optSpaces ~ direction ~ optSpaces ~ rulePart.? ~ optSpaces  ~ coeffs).map{

    case (n, leftOpt, BothDirections, rightOpt, (c1, c2opt)) =>
      val left = leftOpt.getOrElse(Pattern.empty)
      val right = rightOpt.getOrElse(Pattern.empty)

      val name = n.getOrElse(left + " "+ "<->" + " "+right)
      Rule(name, left, right, c1, c2opt)

    case (n, leftOpt, Left2Right, rightOpt, (c1, c2opt)) =>
      val left = leftOpt.getOrElse(Pattern.empty)
      val right = rightOpt.getOrElse(Pattern.empty)

      val name = n.getOrElse(left + " "+"->" + " "+right)
      Rule(name, left, right, c1, c2opt)

    case (n, leftOpt, Right2Left, rightOpt, (c1, c2opt)) =>
      val left = leftOpt.getOrElse(Pattern.empty)
      val right = rightOpt.getOrElse(Pattern.empty)

      val name = n.getOrElse(left + " "+"<-" + " "+right)
      Rule(name, left, right, c1, c2opt) //TODO: fix coefficents
  }

  val observable = P(optSpaces ~ "%obs:" ~ optSpaces ~ label ~spaces ~ rulePart).map{
    case (lb, pat) =>  ObservablePattern(lb, pat)
  }

  /*
  val initNumber = P(optSpaces ~ "%init:" ~ optSpaces ~ labelOrNumber ~ optSpaces ~ rulePart).map{
    case (Left(label),)
    case (Right(number), )
  }
  */


  /*
  val rule = P("'" ~ text.! ~ "'" ~ rulePart ~ direction ~ rulePart ).map{
    case (name, left, BothDirections, right) => Rule(name, left, right, 0)
    case (name, left, Left2Right, right) => Rule(name, left, right, 0)
    case (name, left, Right2Left, right) => Rule(name, left, right, 0)
  }
  */


  //val leftSide =

}
