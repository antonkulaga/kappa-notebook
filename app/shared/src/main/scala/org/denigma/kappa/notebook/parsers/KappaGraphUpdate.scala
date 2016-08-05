package org.denigma.kappa.notebook.parsers

import org.denigma.kappa.model.KappaModel._

object ParsedLine {
  lazy val empty: ParsedLine = ParsedLine("", EmptyKappaElement)
}
case class ParsedLine(line: String, element: KappaElement)


object GraphUpdate {
  lazy val empty = GraphUpdate("", Pattern.empty, Pattern.empty, isRule = false)

  def fromParsedLine(p: ParsedLine) = p match {
    case ParsedLine(line, element: Agent) => GraphUpdate(line, Pattern.apply(List(element)), Pattern.empty, isRule = false)
    case ParsedLine(line, element: InitCondition) => GraphUpdate(line, element.pattern, Pattern.empty, isRule = false)
    case ParsedLine(line, element: ObservablePattern) => GraphUpdate(line, element.pattern, Pattern.empty, isRule = false)
    case ParsedLine(line, element: Rule) => GraphUpdate(line, element.left, element.right, isRule = true)
    case _ => GraphUpdate.empty
  }
}
case class GraphUpdate(line: String, left: Pattern, right: Pattern, isRule: Boolean) {
  lazy val sameAgents = if(isRule) {left.sameAgents(right)} else left.agents.zip(left.agents)
  lazy val unchangedAgents = sameAgents.collect{ case (one, two) if one ==two => one}.toSet
  lazy val updatedAgents =  sameAgents.collect{ case (one, two) if one != two => one -> two}.toSet
  lazy val modifiedAgents = sameAgents.filterNot{ case (one, two)=> one==two}.unzip{case (a, b)=> (a, b)}
  lazy val leftModified = modifiedAgents._1.toSet
  lazy val rightModified = modifiedAgents._2.toSet

  lazy val removedAgents = left.agents.filterNot(p => leftModified.contains(p) || unchangedAgents.contains(p) ).toSet


  lazy val addedAgents = right.agents.filterNot(p => rightModified.contains(p) || unchangedAgents.contains(p) ).toSet

}