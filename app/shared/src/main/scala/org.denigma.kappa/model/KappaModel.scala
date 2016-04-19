package org.denigma.kappa.model

object KappaModel {

  trait KappaElement

  sealed trait Direction extends KappaElement
  case object Left2Right extends Direction
  case object Right2Left extends Direction
  case object BothDirections extends Direction

  case class Pattern(agents: Set[Agent]) extends KappaElement

  case class Rule(name: String, left: Pattern, right: Pattern, forward: Double, backward: Option[Double] = None) {
    val added = left.agents.diff(right.agents)
    val removed = right.agents.diff(left.agents)

    def atomic: Boolean = added.size==1 || removed.size == 1
  }

  case class State(name: String) extends KappaElement

  case class Side(name: String, states: Set[State] = Set.empty) extends KappaElement {
    def ~(state: State): Side = {
      copy(states = states + state)
    }

  }
  object Agent {
    implicit val ordering = new Ordering[Agent] {
      override def compare(x: Agent, y: Agent): Int = x.name.compare(y.name)
    }
  }

  case class Agent(name: String, sides: List[Side] = List.empty) extends KappaElement
  {
    lazy val sideSet = sides.toSet
  }

}


