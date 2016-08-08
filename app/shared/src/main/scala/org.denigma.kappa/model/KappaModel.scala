package org.denigma.kappa.model
import scala.collection.immutable._

object KappaModel {

  trait KappaElement

  sealed trait Direction extends KappaElement
  case object Left2Right extends Direction
  case object Right2Left extends Direction
  case object BothDirections extends Direction

  object Link {
    implicit val ordering: Ordering[Link] = new Ordering[Link] {
      override def compare(x: Link, y: Link): Int = x.label.compare(y.label) match {
        case 0 => x.hashCode().compare(y.hashCode())
        case other => other
      }
    }
  }

  case class Link(fromAgent: Agent, toAgent: Agent, fromSide: Site, toSide: Site, label: String) extends KappaNamedElement
  {
    def name = label

    require(fromAgent.sites.contains(fromSide), s"from Agent($fromAgent) should contain fromSide($fromSide)")

    require(toAgent.sites.contains(toSide), s"to Agent($toAgent) should contain toSide($toSide)")

  }

  object Pattern {
    lazy val empty = Pattern(Nil)
  }

  case class Pattern(agents: List[Agent]) extends KappaElement
  {

    protected def isNamed(key: String) = key != "_" && key !="?"

    protected lazy val linkTuples: List[((Agent, Site, String), Int)] = agents.zipWithIndex.flatMap{
      case (a, i) => a.outgoingLinks.map(v=>(v, i))
    }

    protected lazy val pairLinkTuples: List[((Agent, Site, String), Int)] = linkTuples.filter{
      case ((_, _, "_") , _) | ((_, _, "?"), _) => false
      case _ => true
    }

    //lazy val allLinks: List[Link] = pairLinks ++ wildcardLinks ++ questionLinks

    lazy val pairLinksIndexed: List[(Link, (Int, Int))] = pairLinkTuples.groupBy{ case ((_, _, l), _) => l}.collect{
      case (label, ((a1, s1, _), i1)::((a2, s2, _), i2)::Nil) => (KappaModel.Link(a1, a2, s1, s2, label), (i1, i2))
    }.toList

    lazy val wildcardLinks = linkTuples.collect{ case ((a, s, "_"), _) => KappaModel.Link(a, Agent.wildcard, s, Site.wildcard, "_")}
    lazy val questionLinks = linkTuples.collect{ case ((a, s, "?"), _) => KappaModel.Link(a, Agent.questionable, s, Site.question, "?")}


    private def sameAgent(one: Agent, two: Agent): Boolean = {
      one.name==two.name && one.siteNames == two.siteNames
    }

    def sameAgents(pat: Pattern): List[(Agent, Agent)] = agents.zip(pat.agents).takeWhile(ab=>sameAgent(ab._1, ab._2))

  }

  case class Rule(name: String, left: Pattern, right: Pattern, forward: Either[String, Double], backward: Option[Either[String, Double]] = None) extends KappaNamedElement
  {
    lazy val same = left.sameAgents(right)

    lazy val removed = if(same.length==left.agents.length) Nil else left.agents.takeRight(left.agents.length - same.length)

    lazy val added = if(same.length==right.agents.length) Nil else right.agents.takeRight(right.agents.length - same.length)

    lazy val modified = same.filter{
      case (one, two)=> one.sites != two.sites
    }

    lazy val modifiedLeft = modified.map(_._1)

    lazy val modifiedRight = modified.map(_._1)

    def direction: Direction = if(backward.isEmpty) KappaModel.Left2Right else KappaModel.BothDirections
    //lazy val kept = right
    //def atomic: Boolean = added.size==1 || removed.size == 1
  }

  case object EmptyKappaElement extends KappaElement


  trait KappaNamedElement extends KappaElement{
    def name: String
  }

  case class State(name: String) extends KappaNamedElement

  object Site {
    lazy val wildcard = Site("_")
    lazy val question = Site("?")
  }

  case class Site(name: String, states: Set[State] = Set.empty, links: Set[String] = Set.empty) extends KappaNamedElement {
    def ~(state: State): Site = {
      copy(states = states + state)
    }

  }
  object Agent {
    implicit val ordering: Ordering[Agent] = new Ordering[Agent] {
      override def compare(x: Agent, y: Agent): Int = x.name.compare(y.name) match {
        case 0 => x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
        case other => other
      }
    }

    lazy val wildcard = Agent("_", Set(Site("_")))
    lazy val questionable = Agent("?", Set(Site("?")))
  }

  case class Agent(name: String, sites: Set[Site] = Set.empty) extends KappaNamedElement
  {

    lazy val siteNames: Set[String] = sites.map(s=>s.name)

    lazy val outgoingLinks: Set[(Agent, Site, String)] = {
      for{
        s <- sites
        l <- s.links
      } yield  (this, s, l)
    }
  }

  case class ObservablePattern(name: String, pattern: Pattern) extends KappaNamedElement


  case class InitCondition(number: Either[String, Double], pattern: Pattern) extends KappaElement

}


