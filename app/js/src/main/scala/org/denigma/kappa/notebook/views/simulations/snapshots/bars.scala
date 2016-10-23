package org.denigma.kappa.notebook.views.simulations.snapshots

import org.denigma.kappa.model.KappaModel.Pattern


case class PatternGroupBar(data: Map[Pattern, Int]) extends Bar {
  type Data = Map[Pattern, Int]

  lazy val quantity: Double = data.foldLeft(0){ case (acc, (pat, q)) =>  acc + q}

  lazy val name = quantity.toString
}

case class PatternBar(data: Pattern, quantity: Double) extends Bar {
  type Data = Pattern

  def name = data.toKappaCode

}

trait Bar{
  type Data
  def data: Data
  def quantity: Double
  def name: String
}

