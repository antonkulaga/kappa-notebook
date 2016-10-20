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

    def embeddingsOf(subpattern: Pattern): Map[Pattern, Int] = patterns.filter{
      case (pattern, q) => pattern.agents.indices.exists{ i => subpattern.embedsInto(pattern)  }
    }

    def debugEmbeddingsOf(subpattern: Pattern): Map[Pattern, Int] = {
      pprint.pprintln(s"PATTERNS LENGTH IS ${patterns.size} ====")
      patterns.filter{
        case (pattern, q) =>
          val result = pattern.agents.indices.exists{ i => subpattern.embedsInto(pattern) }
          pprint.pprintln(s"RESULT($result) OF EMBEDING OF \n${subpattern} \n INTO\n ${pattern}\n")
          //pprint.pprintln(pattern.agents)
          result
      }
    }
  }

  /**
    *
    * @param _agents real agents
    * @param virtualAgents virtual agents, like ?_ or !_
    */
  case class Pattern private(_agents: List[Agent], virtualAgents: Set[Agent]) extends KappaElement with WithKappaCode
  {


    def isEmpty = agents.isEmpty

    lazy val toKappaCode = _agents.foldLeft(""){
      case ("", ag) => ag.toKappaCode
      case (acc, ag) => acc + ", " + ag.toKappaCode
    }

    /*
    def head = agents.head
    def headOption = agents.headOption
    def tail = Pattern(agents.tail)
    */

    def embedsInto(pat: Pattern): Boolean = {
      pat.agents.indices.exists{ i => embedAgentInto(0, i, pat, Set.empty[(Int, Int)]) }
    }

    protected def embedAgentInto(currentNum: Int, targetNum: Int, targetPattern: Pattern, previous: Set[(Int, Int)]): Boolean = {
      require(targetPattern.agents.size > targetNum, s"index out of bounds inside $targetPattern")
      require(agents.size> currentNum, s"index out of bounds inside $this")
      val currentAgent = agents(currentNum)
      val target = targetPattern.agents(targetNum)
      currentAgent.embedsInto(target) match {
        case true if currentAgent.outgoingLinks.isEmpty => true
        case false => false
        case true =>
          currentAgent.siteNames.forall{
            s =>
              val prev = previous.+((currentNum, targetNum))
              if(!linkMap.contains( (currentNum, s))){
                pprint.pprintln(s"cannot find CURRENT link for ${(currentNum, s)} in:")
                pprint.pprintln(linkMap)
                pprint.pprintln("AGENTS ARE:")
                pprint.pprintln(agents)
                pprint.pprintln("TUPLES ARE:")
                pprint.pprintln(outgoingTuples)
                pprint.pprintln("LINKS ARE:")
                pprint.pprintln(links)
                pprint.pprintln(currentAgent)
              }
              val (curOut, curSite): (Int, String) = linkMap( (currentNum, s) )
              if(!targetPattern.linkMap.contains( (targetNum, s))){
                pprint.pprintln(s"cannot find TURGET link for ${(targetNum, s)} in:")
                pprint.pprintln(linkMap)
              }
              val (targetOut, tarSite): (Int, String) = targetPattern.linkMap( (targetNum, s) )
              val p = previous.contains((curOut, targetOut))
              p || embedAgentInto(curOut, targetOut, targetPattern, prev)
          }
      }
    }

    protected def traverse(currentAgent: Agent) = {
      require(agents.contains(currentAgent), "we can traverse only from the agent that belongs to the pattern")

    }

    /*

    //NOTE: IS BUGGY, I USE IT ONLY FOR SIMPLE SNAPSHOTS MATCHING
    def embedsInto(pat: Pattern) = {
      pat.agents.sliding(agents.length, 1).exists{ ags =>
        val zp = agents.zip(ags)
        val result = ags.length == agents.length  &&
        zp.forall{ case (a, b) => a.embedsInto(b) } && links.subsetOf(Pattern(ags).links)
        if(agents.length > 1) {
          pprint.pprintln(s"embeding of:====")
          pprint.pprintln(agents)
          pprint.pprintln("==into==")
          pprint.pprintln(ags)
          pprint.pprintln(s"RESULT IS ${result}")
          if(!result){
            val aResult = zp.forall{ case (a, b) => a.embedsInto(b) }
            pprint.pprintln("agent comparison = "+ aResult)
            pprint.pprintln("link comparison = " + {links.subsetOf(Pattern(ags).links)})
            if(!aResult) zp.foreach{ case (a, b) => a.debugEmbedsInto(b) }
          }
        }
        result
      }
    }
    */


    def embedsIntoCount(pat: Pattern) = {
      pat.agents.indices.count{ i => embedAgentInto(0, i, pat, Set.empty[(Int, Int)]) }
    }

    lazy val agents = _agents ++ virtualAgents

    lazy val outgoingTuples: List[(String, String, Int)] = agents.flatMap{ ag => ag.outgoingLinks }

    protected lazy val grouped = outgoingTuples.groupBy{
      case (link, site, agNum) => link
    }

    lazy val linkMap = links.map{ case (siteFrom, fromNum, siteTo, toNum) => (fromNum, siteFrom) -> (toNum, siteTo) }.toMap


    /**
      * Lins of format: site -> (SiteFrom, FromNum, SiteTo, ToNum)
      */
    lazy val links: Set[(String, Int, String, Int)] = grouped.collect{
      case (Agent.wildcard.name, ls) => ls.map{ case (_, siteFrom, fromNum) =>(siteFrom, fromNum, Agent.wildcard.name, Agent.wildcard.position)}.toSet
      case (Agent.questionable.name, ls) => ls.map{ case (_, siteFrom, fromNum) =>(siteFrom, fromNum, Agent.questionable.name, Agent.questionable.position)}.toSet
      case (link, (_, siteFrom, fromNum)::(_, siteTo, toNum)::Nil) =>
        Set((siteFrom, fromNum, siteTo, toNum)
          ,(siteTo, toNum,siteFrom, fromNum) //temporaly fix
        )
    }.flatMap(v=>v).toSet

    def sameAgents(pat: Pattern) = agents.zip(pat.agents).takeWhile{ case (a, b) => a.sameAs(b)}

  }

  case class Rule(name: String, left: Pattern, right: Pattern, forward: Either[String, Double], backward: Option[Either[String, Double]] = None)
    extends KappaNamedElement with WithKappaCode
  {

    private lazy val mainPart = s"${if(name.isEmpty) "" else s"'$name'"} "+ left.toKappaCode + right.toKappaCode

    lazy val toKappaCode = {
      val left = s"${if(name.isEmpty) "" else s"'$name'"} "
      val kOn = forward.fold(s=> s"'${s}'", _.toString )
      backward match {
        case Some(kOff) => left +" <-> " + right.toKappaCode +" @ " + kOn + ", " + kOff.fold(s=> s"'${s}'", _.toString )
        case None => left + " -> " + right.toKappaCode + " @ " + kOn
      }
    }

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

  trait WithKappaCode extends KappaElement
  {
    def toKappaCode: String
  }

  case class State(name: String) extends KappaNamedElement

  object Site {
    lazy val wildcard = Site("_")
    lazy val question = Site("?")
  }

  case class Site(name: String, states: Set[State] = Set.empty, links: Set[String] = Set.empty) extends KappaNamedElement with WithKappaCode{
    def ~(state: State): Site = {
      copy(states = states + state)
    }

    lazy val toKappaCode = s"$name" + states.foldLeft(""){ case (acc, e) => acc + "~" + e.name} + links.foldLeft(""){ case (acc, e) => acc + "!" + e} //note check validity

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

  case class Agent(name: String, sites: Set[Site] = Set.empty, position: Int = -1) extends KappaNamedElement with WithKappaCode
  {

    private lazy val sitesCode = sites.foldLeft(""){ case (acc, s) => acc + s.toKappaCode}

    lazy val toKappaCode = s"$name($sitesCode)"


    /**
      * @param otherAgent
      * @return
      */
    def embedsInto(otherAgent: Agent): Boolean = {
      name == otherAgent.name && sites.forall{site=>
        otherAgent.sites.exists(s => s.name == site.name && site.states.subsetOf(s.states) && site.links.size <= s.links.size)
      }
    }

    def debugEmbedsInto(otherAgent: Agent): Boolean = {

      pprint.pprintln(s"ME IS ${this}")
      pprint.pprintln(s"OTHER IS ${otherAgent}")
      pprint.pprintln(s"names ${name == otherAgent.name}")
      pprint.pprintln(s"site names ${
        sites.forall{site=>
          otherAgent.sites.exists(otherSite =>
            otherSite.name == site.name)
        }
      }")
      pprint.pprintln(s"states ${
        sites.forall{site=>
          otherAgent.sites.exists(otherSite =>
            otherSite.name == site.name &&
              site.states.subsetOf(otherSite.states))
        }
      }")
      pprint.pprintln(s"links ${
        sites.forall{site=>
          otherAgent.sites.exists(otherSite =>
            otherSite.name == site.name &&
              site.states.subsetOf(otherSite.states) &&
              site.links.size <= otherSite.links.size)
        }
      }")
      val result =
      name == otherAgent.name &&
        sites.forall{site=>
          otherAgent.sites.exists(otherSite =>
            otherSite.name == site.name &&
              site.states.subsetOf(otherSite.states) &&
              site.links.size <= otherSite.links.size)
        }

      pprint.pprintln(s"RESULT IS ${result}")
      result
    }

    lazy val siteNames: Set[String] = sites.map(s=>s.name)

    lazy val linkSiteMap: Map[String, Site] = sites.flatMap(s=> s.links.map(l=> (l , s))).toMap

    lazy val outgoingLinks: Set[(String, String, Int)] = sites.flatMap(s => s.links.map(l => (l, s.name, position)))

    def hasPosition = position >= 0

    def sameAs(other: Agent) = name ==other.name && position == other.position && siteNames == other.siteNames
  }

  case class ObservablePattern(name: String, pattern: Pattern) extends KappaNamedElement


  case class InitCondition(number: Either[String, Double], pattern: Pattern) extends KappaElement with WithKappaCode {
    lazy val toKappaCode = "%init: "+pattern.toKappaCode
  }

}


