package org.denigma.kappa.notebook.views.visual.rules
import org.denigma.kappa.model.KappaModel._
import org.denigma.kappa.notebook.parsers.{GraphUpdate, ParsedLine}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._


class GraphUpdater(val parsed: Rx[ParsedLine]) {

  val update: Rx[GraphUpdate] = parsed.map{ p => GraphUpdate.fromParsedLine(p)}

  val sameAgents: Rx[List[(Agent, Agent)]] = update.map(u=>u.sameAgents)

  val unchangedAgents: Rx[Set[Agent]] = update.map(u=>u.unchangedAgents)

  val updatedAgents: Rx[Set[(Agent, Agent)]] = update.map(u => u.updatedAgents)

  val modifiedAgents: Rx[(List[Agent], List[Agent])] = update.map(u => u.modifiedAgents)

  val leftModified = update.map(u => u.leftModified)
  val rightModified =  update.map(u => u.rightModified)

  val removedAgents: Rx[Set[Agent]] = update.map(u => u.removedAgents)

  val addedAgents: Rx[Set[Agent]] =  update.map(u => u.addedAgents)

}
