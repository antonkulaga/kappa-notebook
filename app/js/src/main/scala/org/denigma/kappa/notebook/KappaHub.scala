package org.denigma.kappa.notebook

import org.denigma.codemirror.PositionLike
import org.denigma.controls.papers.Bookmark
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Var

import scala.collection.immutable._
import org.denigma.kappa.messages
import org.denigma.kappa.messages.{KappaPath, _}

import scala.Predef.Map

object KappaHub{

  private val testMap: Map[String, Bookmark] = Map( ("/files/ossilator/Stricker08.pdf", Bookmark("/files/ossilator/Stricker08.pdf", 1)))

  def empty: KappaHub = KappaHub(
    //Var(KappaProject()),
    Var("HelloWorld.ka"),
    Var(KappaPath.empty),
    Var(PositionLike.empty),
    Var(Defaults.code),
    Var(messages.Defaults.simulations),
    Var(messages.Defaults.runModel),
    Var(List.empty[String]),
    Var(testMap)
  )
}

/**
  * Created by antonkulaga on 07/04/16.
  */
case class KappaHub(
                     name: Var[String],
                     path: Var[KappaPath],
                     kappaCursor: Var[PositionLike],
                     kappaCode: Var[Code],
                     //kappaCode: Var[Map[String, WebSim.Code]],
                     simulations: Var[Map[(Int, RunModel), SimulationStatus]],
                     runParameters: Var[RunModel],
                     errors: Var[List[String]],
                     papers: Var[Map[String, Bookmark]]
)