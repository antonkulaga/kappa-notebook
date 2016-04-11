package org.denigma.kappa.notebook

import org.denigma.codemirror.PositionLike
import org.denigma.controls.papers.Bookmark
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Var
import scala.collection.immutable._
import org.denigma.kappa.WebSim

object KappaHub{
  def empty: KappaHub = KappaHub(
    Var("HelloWorld.ka"),
    Var(PositionLike.empty),
    Var(WebSim.Defaults.code),
    Var(WebSim.Defaults.simulations),
    Var(WebSim.Defaults.runModel),
    Var(List.empty[String])
  )
}

/**
  * Created by antonkulaga on 07/04/16.
  */
case class KappaHub(
                     name: Var[String],
                     kappaCursor: Var[PositionLike],
                     kappaCode: Var[WebSim.Code],
                     //kappaCode: Var[Map[String, WebSim.Code]],
                     simulations: Var[Map[(Int, WebSim.RunModel), WebSim.SimulationStatus]],
                     runParameters: Var[WebSim.RunModel],
                     errors: Var[List[String]],
                     paperLocation: Var[Bookmark] = Var(Bookmark("/files/ossilator/Stricker08.pdf", 1)) ///*Var(Bookmark("", 0, Nil)*/ //"/resources/models/Stricker08.pdf"
)