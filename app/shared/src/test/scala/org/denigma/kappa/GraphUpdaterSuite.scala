package org.denigma.kappa

import fastparse.core.Parsed
import fastparse.core.Parsed.Success
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.parsers.{CommentLinksParser, GraphUpdate, KappaParser, ParsedLine}
import org.scalatest.{Inside, Matchers, WordSpec}

import scala.collection.immutable._
/**
  * Created by antonkulaga on 06/03/16.
  */
class GraphUpdaterSuite extends WordSpec with Matchers with Inside  {

  "Graph updater" should {


    "understand complex agents" in {
      import KappaModel._
      val parser = new KappaParser
      lazy val states = Set(
        Site("downstream"), Site("upstream"), Site("binding"),
        Site("type", Set(State("BBaB0034"), State("BBaC0012"), State("BBaC0040"),
          State("BBaC0051"), State("BBaR0010"), State("BBaR0040"), State("BBaR0051"))) )
      val rnaAgent = KappaModel.Agent("RNA", states )
      val pattern = parser.mergeLine(
        """
          |%agent: RNA(downstream,upstream,type~BBaB0034~BBaC0012~BBaC0040~BBaC0051~BBaR0010~BBaR0040~BBaR0051,binding)
        """).stripMargin
      val res: Agent = parser.agentDecl.parse(pattern).get.value
      val update = GraphUpdate.fromParsedLine(ParsedLine(pattern, res))
      update.sameAgents.size shouldEqual 1
      update.isRule shouldEqual false
      update.unchangedAgents shouldEqual Set(rnaAgent)
      update.addedAgents.isEmpty shouldEqual true
      update.removedAgents.isEmpty shouldEqual true
      update.updatedAgents.isEmpty shouldEqual true
      update.leftModified.isEmpty shouldEqual true
      update.rightModified.isEmpty shouldEqual true
    }
    /*

    "understand simple rules" in {
      import KappaModel._
      val parser = new KappaParser
      val pTetLeft = Agent("pTet", Set(Site("binding", Set.empty, Set("1"))))
      val tetRLeft = Agent("TetR", Set(Site("dna", Set.empty, Set("1"))))
      val pTetRight = Agent("pTet", Set(Site("binding", Set.empty, Set.empty)))

      val rule = parser.mergeLine(
        """
          |'tetR.degradation2' pTet(binding!1),TetR(dna!1) ->  pTet(binding) @ 'degrad2'
        """.stripMargin)
      val res: Rule = parser.rule.parse(pattern).get.value
      val update = GraphUpdate.fromParsedLine(ParsedLine(pattern, res))
      update.sameAgents.size shouldEqual 1
      update.isRule shouldEqual false
      update.unchangedAgents shouldEqual Set(rnaAgent)
      update.addedAgents.isEmpty shouldEqual true
      update.removedAgents.isEmpty shouldEqual true
      update.updatedAgents.isEmpty shouldEqual true
      update.leftModified.isEmpty shouldEqual true
      update.rightModified.isEmpty shouldEqual true
    }

    "understand init complex conditions" in {
      import KappaModel._
      val parser = new KappaParser
      val init = parser.mergeLine("""
        |%init: 'operon count' DNA(upstream,downstream!4,binding,type~BBaR0051p1), DNA(upstream!4,downstream!5,binding,type~BBaR0051p2), DNA(upstream!5,downstream!6,binding,type~BBaR0051p3), \
        |                       DNA(upstream!6,downstream!7,binding,type~BBaR0051p4), DNA(upstream!7,downstream!8,binding,type~BBaB0034), DNA(upstream!8,downstream!9,binding,type~BBaC0012), \
        |                       DNA(upstream!9,downstream,binding,type~BBaB0011)
      """.stripMargin)
    }

    "understand complex rules" in {
      import KappaModel._
      val parser = new KappaParser
      val rule = parser.mergeLine("""
        |'LacI binding to R0010p2 (no LacI)' \
        |	DNA(binding,type~BBaR0010p3,upstream!2), LacI(dna,lactose), DNA(downstream!2,binding,type~BBaR0010p2) -> \
        |	DNA(binding,type~BBaR0010p3,upstream!3), LacI(dna!1,lactose), DNA(downstream!3,binding!1,type~BBaR0010p2) @ 'transcription factor binding rate'
      """.stripMargin)
    }
    */
  }

}