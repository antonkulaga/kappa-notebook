package org.denigma.kappa.model

object KappaModel {

  trait KappaElement

  sealed trait Direction extends KappaElement
  case object Left2Right extends Direction
  case object Right2Left extends Direction
  case object BothDirections extends Direction

  object Link {
    implicit val ordering = new Ordering[Link] {
      override def compare(x: Link, y: Link): Int = x.label.compare(y.label) match {
        case 0 => x.hashCode().compare(y.hashCode())
        case other => other
      }
    }
  }

  case class Link(fromAgent: Agent, toAgent: Agent, fromSide: Side, toSide: Side, label: String) extends KappaElement
  {
    require(fromAgent.sideSet.contains(fromSide), s"from Agent($fromAgent) should contain fromSide($fromSide)")

    require(toAgent.sideSet.contains(toSide), s"from Agent($toAgent) should contain fromSide($toSide)")

  }
  case class Pattern(agents: List[Agent]) extends KappaElement
  {
    protected def isNamed(key: String) = key != "_" && key !="?"

    protected lazy val linkTuples: List[(String, (Side, Agent))] = for{
      a <- agents
      (name, side) <-a.links
    } yield(name, (side, a))

    //TODO: think about a potential bug with two ? and _
    lazy val allLinks: Map[String, List[(Side, Agent)]] = linkTuples.groupBy(_._1).map{ case (key, value) => key -> value.map(v=>v._2)}

    lazy val danglingLinks: Map[String, List[(Side, Agent)]] = allLinks.collect{
      case (key, value) if value.length < 2 && isNamed(key) => key -> value
    }

    lazy val duplicatedLinks: Map[String, List[(Side, Agent)]] = allLinks.collect{
      case (key, value) if value.length > 2 && isNamed(key) => key -> value
    }

    lazy val linksSomewhere = allLinks.getOrElse("_", List.empty)

    lazy val unclearLinks = allLinks.getOrElse("?", List.empty)

    lazy val links: Map[String, Link] = allLinks.collect {
      case (key, (side1, agent1)::(side2, agent2)::Nil)  =>
        key -> KappaModel.Link(agent1, agent2, side1, side2, key)
    }
  }

  case class Rule(name: String, left: Pattern, right: Pattern, forward: Either[String, Double], backward: Option[Either[String, Double]] = None) {
    lazy val added = left.agents.diff(right.agents)
    lazy val removed = right.agents.diff(left.agents)
    //lazy val kept = right
    //def atomic: Boolean = added.size==1 || removed.size == 1
  }

  case class State(name: String) extends KappaElement

  case class Side(name: String, states: Set[State] = Set.empty, links: Set[String] = Set.empty) extends KappaElement {
    def ~(state: State): Side = {
      copy(states = states + state)
    }

  }
  object Agent {
    implicit val ordering = new Ordering[Agent] {
      override def compare(x: Agent, y: Agent): Int = x.name.compare(y.name) match {
        case 0 => x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
        case other => other
      }
    }
  }

  case class Agent(name: String, sides: List[Side] = List.empty) extends KappaElement
  {
    lazy val sideSet = sides.toSet

    lazy val links: List[(String, Side)] = for {
        s <- sides
        l <- s.links
      } yield l -> s

    lazy val linkMap = links.toMap

    def hasDuplicates = links.size > linkMap.size
  }

}


