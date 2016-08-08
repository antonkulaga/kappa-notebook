package org.denigma.kappa

// test-only org.denigma.kappa.GraphUpdaterSuite


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


    "understand simple rules" in {
      import KappaModel._
      val parser = new KappaParser
      val pTetLeft = Agent("pTet", Set(Site("binding", Set.empty, Set("1"))))
      val tetRLeft = Agent("TetR", Set(Site("dna", Set.empty, Set("1"))))
      val pTetRight = Agent("pTet", Set(Site("binding", Set.empty, Set.empty)))

      val text = parser.mergeLine(
        """
          |'tetR.degradation2' pTet(binding!1),TetR(dna!1) ->  pTet(binding) @ 'degrad2'
        """.stripMargin)
      val res: Rule = parser.rule.parse(text).get.value
      val update = GraphUpdate.fromParsedLine(ParsedLine(text, res))
      update.sameAgents.size shouldEqual 1
      update.isRule shouldEqual true
      update.unchangedAgents shouldEqual Set.empty
      update.addedAgents.isEmpty shouldEqual true
      update.removedAgents shouldEqual Set(tetRLeft)
      update.updatedAgents shouldEqual Set((pTetLeft, pTetRight))
      update.leftModified shouldEqual Set(pTetLeft)
      update.rightModified shouldEqual Set(pTetRight)
    }


    "understand complex rules" in {
      import KappaModel._
      val dnaLeft1 = Agent("DNA", Set(Site("binding"), Site("type", Set(State("BBaR0010p3"))), Site("upstream", Set.empty, Set("2"))))
      val lacILeft = Agent("LacI", Set(Site("dna"), Site("lactose")))
      val dnaLeft2 = Agent("DNA", Set(
        Site("downstream", Set.empty, Set("2")), Site("binding"), Site("type", Set(State("BBaR0010p2")))
      ))
      val dnaRight1 = Agent("DNA", Set(Site("binding"), Site("type", Set(State("BBaR0010p3"))), Site("upstream", Set.empty, Set("3"))))
      val lacIRight = Agent("LacI", Set(Site("dna", Set.empty, Set("1")), Site("lactose")))
      val dnaRight2 = Agent("DNA", Set(
        Site("downstream", Set.empty, Set("3")), Site("binding", Set.empty, Set("1")), Site("type", Set(State("BBaR0010p2")))
      ))
      val parser = new KappaParser
      val text = parser.mergeLine("""
        |'LacI binding to R0010p2 (no LacI)' \
        |	DNA(binding,type~BBaR0010p3,upstream!2), LacI(dna,lactose), DNA(downstream!2,binding,type~BBaR0010p2) -> \
        |	DNA(binding,type~BBaR0010p3,upstream!3), LacI(dna!1,lactose), DNA(downstream!3,binding!1,type~BBaR0010p2) @ 'transcription factor binding rate'
      """.stripMargin)
      val res: Rule = parser.rule.parse(text).get.value
      val update = GraphUpdate.fromParsedLine(ParsedLine(text, res))
      update.sameAgents.size shouldEqual 3
      update.isRule shouldEqual true
      update.unchangedAgents shouldEqual Set.empty
      update.addedAgents.isEmpty shouldEqual true
      update.removedAgents.isEmpty shouldEqual true
      update.updatedAgents shouldEqual Set(
        (dnaLeft1, dnaRight1), (lacILeft, lacIRight), (dnaLeft2, dnaRight2)
      )
      update.leftModified.isEmpty shouldEqual false
      update.rightModified.isEmpty shouldEqual false
    }

    /*
    "understand complex links" in {
      import KappaModel._
      val parser = new KappaParser
      val text = parser.mergeLine("""
                                    |'LacI binding to R0010p2 (no LacI)' \
                                    |	DNA(binding,type~BBaR0010p3,upstream!2), LacI(dna,lactose), DNA(downstream!2,binding,type~BBaR0010p2) -> \
                                    |	DNA(binding,type~BBaR0010p3,upstream!3), LacI(dna!1,lactose), DNA(downstream!3,binding!1,type~BBaR0010p2) @ 'transcription factor binding rate'
                                  """.stripMargin)
      val res: Rule = parser.rule.parse(text).get.value
      val update = GraphUpdate.fromParsedLine(ParsedLine(text, res))
      update.link
    }
    */
  }

}