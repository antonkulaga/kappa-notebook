package org.denigma.kappa.parsers

import org.denigma.kappa.model.KappaModel._

import scala.Predef.Set
import scala.collection.immutable._

object ParsedLine {
  lazy val empty: ParsedLine = ParsedLine("", EmptyKappaElement)
}
case class ParsedLine(line: String, element: KappaElement)


case class GraphUpdateInfo(addedAgents: Set[Agent],
                           removedAgents: Set[Agent],
                           unchangedAgents: Set[Agent],
                           updatedAgents: Set[(Agent, Agent)],
                           addedLinks: Set[GraphUpdate.Link],
                           removedLinks: Set[GraphUpdate.Link],
                           unchangedLinks: Set[GraphUpdate.Link]
                          )

object GraphUpdate {

  type Link = (String, Int, String, Int)

  lazy val empty = GraphUpdate("", Right(Pattern.empty))

  def fromParsedLine(p: ParsedLine) = p match {
    case ParsedLine(line, element: Agent) => GraphUpdate(line, Right(Pattern(List(element))))
    case ParsedLine(line, element: InitCondition) =>  GraphUpdate(line, Right(element.pattern))
    case ParsedLine(line, element: ObservablePattern) => GraphUpdate(line, Right(element.pattern))
    case ParsedLine(line, element: Rule) => GraphUpdate(line, Left(element))
    case _ => GraphUpdate.empty
  }
}

case class GraphUpdate(line: String, ruleOrPattern: Either[Rule, Pattern]) {
  def isRule = ruleOrPattern.isLeft

  lazy val sameAgents: List[(Agent, Agent)] = ruleOrPattern.fold(r => r.same, p => p.agents.zip(p.agents))
  protected lazy val unchangedAgents = ruleOrPattern.fold(r => r.unchanged, p => p.agents).toSet
  protected lazy val updatedAgents  = ruleOrPattern.fold(r => r.modified, p => Nil).toSet
  protected lazy val removedAgents = ruleOrPattern.fold( r => r.removed, p => Nil).toSet
  protected lazy val addedAgents = ruleOrPattern.fold( r => r.added, p => Nil).toSet

  lazy val leftModified = ruleOrPattern.fold( r => r.modifiedLeft, p => Nil).toSet
  lazy val rightModified = ruleOrPattern.fold( r => r.modifiedRight, p => Nil).toSet

  protected lazy val unchangedLinks: Set[(String, Int, String, Int)] = ruleOrPattern.fold( r => r.unchangedLinks, p => p.links)
  protected lazy val removedLinks: Set[(String, Int, String, Int)] = ruleOrPattern.fold( r => r.removedLinks, p =>  Set.empty[(String, Int, String, Int)])
  protected lazy val addedLinks: Set[(String, Int, String, Int)] = ruleOrPattern.fold( r => r.addedLinks, p =>  Set.empty[(String, Int, String, Int)])

  lazy val updateInfo: GraphUpdateInfo = GraphUpdateInfo(addedAgents, removedAgents, unchangedAgents, updatedAgents, addedLinks, removedLinks, unchangedLinks)
}