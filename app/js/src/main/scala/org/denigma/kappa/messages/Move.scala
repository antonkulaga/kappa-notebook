package org.denigma.kappa.messages

object Move {

  object Direction extends Enumeration {
    type Direction = Value
    val LEFT, RIGHT = Value
  }

  case class RelativeTo(origin: String, movable: String, direction: Move.Direction.Direction) extends UIMessage
}