package org.denigma.kappa.model

import scala.collection.immutable._

object KappaModel {

  trait KappaElement

  sealed trait Direction extends KappaElement
  case object Left2Right extends Direction
  case object Right2Left extends Direction
  case object BothDirections extends Direction

  object Pattern {
    lazy val empty = Pattern(Nil)

    def apply(agents: List[Agent]): Pattern = {
      val positionedAgents = agents.zipWithIndex.map{ case (agent, index) => agent.copy(position = index) }
      val extra = agents.collect{
        case a if a.sites.exists(s => s.links.contains("?")) => Agent.questionable
        case a if a.sites.exists(s => s.links.contains("_")) => Agent.wildcard
      }.toSet
      new Pattern(positionedAgents, extra)
    }
  }

  case class KappaSnapshot(name: String, event: Int, patterns: Map[Pattern, Int]) extends KappaNamedElement
  {
    def embeddingsOf(pattern: Pattern) = patterns.filter{
      case (pat, q) => pattern.embedsInto(pat)
    }
  }

  /**
    *
    * @param _agents real agents
    * @param virtualAgents virtual agents, like ?_ or !_
    */
  case class Pattern private(_agents: List[Agent], virtualAgents: Set[Agent]) extends KappaElement
  {

    //NOTE: IS BUGGY, I USE IT ONLY FOR SIMPLE SNAPSHOTS MATCHING
    def embedsInto(pat: Pattern) = {
      pat.agents.sliding(agents.length, 1).exists{ ags =>
        ags.length == agents.length && agents.zip(ags).forall{ case (a, b) => a.embedsInto(b)}
      }
    }

    def embedsCount(pat: Pattern) = {
      pat.agents.sliding(agents.length, 1).count{ ags =>
        agents.zip(ags).forall{ case (a, b) => a.embedsInto(b)}
      }
    }

    lazy val agents = _agents ++ virtualAgents


    def matches(pattern: Pattern) = {
      pattern.agents.collect{
        case ags =>
      }
    }

    protected lazy val outgoingTuples: List[(String, String, Int)] = agents.flatMap{ ag => ag.outgoingLinks }

    protected lazy val grouped = outgoingTuples.groupBy{
      case (link, site, agNum) => link
    }

    lazy val links: Set[(String, Int, String, Int)] = grouped.collect{
      case (Agent.wildcard.name, ls) => ls.map{ case (_, siteFrom, fromNum) =>(siteFrom, fromNum, Agent.wildcard.name, Agent.wildcard.position)}.toSet
      case (Agent.questionable.name, ls) => ls.map{ case (_, siteFrom, fromNum) =>(siteFrom, fromNum, Agent.questionable.name, Agent.questionable.position)}.toSet
      case (link, (_, siteFrom, fromNum)::(_, siteTo, toNum)::Nil) =>Set((siteFrom, fromNum, siteTo, toNum))
    }.flatMap(v=>v).toSet


    /*
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
    */

    def sameAgents(pat: Pattern) = agents.zip(pat.agents).takeWhile{ case (a, b) => a.sameAs(b)}

  }

  case class Rule(name: String, left: Pattern, right: Pattern, forward: Either[String, Double], backward: Option[Either[String, Double]] = None) extends KappaNamedElement
  {

    lazy val same: List[(Agent, Agent)] = left.sameAgents(right)

    lazy val removed = if(same.length==left.agents.length) Nil else left.agents.takeRight(left.agents.length - same.length)

    lazy val added = if(same.length==right.agents.length) Nil else right.agents.takeRight(right.agents.length - same.length)

    lazy val modified: List[(Agent, Agent)] = same.filter{
      case (one, two)=> one.sites != two.sites
    }
    lazy val unchanged = same.filter{  case (one, two) => one.sites == two.sites }.map(_._1)

    lazy val modifiedLeft = modified.map(_._1)

    lazy val modifiedRight = modified.map(_._1)

    lazy val direction: Direction = if(backward.isEmpty) KappaModel.Left2Right else KappaModel.BothDirections

    protected def newNum(num: Int) = if(num < same.length) num else {

    }

    lazy val unchangedLinks: Set[(String, Int, String, Int)] = {
      val l = left.links.take(same.length)
      val r = right.links.take(same.length)
      l.intersect(r)
    }

    lazy val removedLinks: Set[(String, Int, String, Int)] = left.links.diff(unchangedLinks)
    lazy val addedLinks: Set[(String, Int, String, Int)] = right.links.diff(unchangedLinks)



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
    /*
    implicit val ordering: Ordering[Agent] = new Ordering[Agent] {
      override def compare(x: Agent, y: Agent): Int = x.name.compare(y.name) match {
        case 0 =>
          x.position.compare(y.position) match {
            case 0 =>
          }
          x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
        case other => other
      }
    }
    */

    lazy val wildcard = Agent("_", Set(Site("_")), position = Int.MaxValue)
    lazy val questionable = Agent("?", Set(Site("?")), position = Int.MinValue)
  }

  case class Agent(name: String, sites: Set[Site] = Set.empty, position: Int = -1) extends KappaNamedElement
  {

    /**
      * @param otherAgent
      * @return
      */
    def embedsInto(otherAgent: Agent): Boolean = name == otherAgent.name && sites.subsetOf(otherAgent.sites)

    lazy val siteNames: Set[String] = sites.map(s=>s.name)

    lazy val outgoingLinks: Set[(String, String, Int)] = sites.flatMap(s => s.links.map(l => (l, s.name, position)))

    def hasPosition = position >= 0
    
    def sameAs(other: Agent) = name ==other.name && position == other.position && siteNames == other.siteNames
  }

  case class ObservablePattern(name: String, pattern: Pattern) extends KappaNamedElement


  case class InitCondition(number: Either[String, Double], pattern: Pattern) extends KappaElement

}


