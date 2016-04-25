package org.denigma.kappa.notebook

import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.papers.Bookmark
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Var

import scala.collection.immutable._
import org.denigma.kappa.messages
import org.denigma.kappa.messages.{KappaPath, _}

import scala.Predef.Map

object KappaHub{

  private val testMap: Map[String, Bookmark] = Map(
    ("/files/ossilator/Stricker08.pdf", Bookmark("/files/ossilator/Stricker08.pdf", 1))/*,
     ( "/files/repressilator/Repressilator.pdf", Bookmark("/files/repressilator/Repressilator.pdf", 1)),
      ("/files/ossilator/nature07389-s01.pdf", Bookmark("/files/ossilator/nature07389-s01.pdf", 1))*/
    )

  def empty: KappaHub = KappaHub(
    //Var(KappaProject()),
    Var("HelloWorld.ka"),
    //Var(KappaPath.empty),
    Var(KappaFolder.empty),
    Var(None),
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
                     kappaCursor: Var[Option[(Editor, PositionLike)]],
                     kappaCode: Var[Code],
                     //kappaCode: Var[Map[String, WebSim.Code]],
                     simulations: Var[Map[(Int, RunModel), SimulationStatus]],
                     runParameters: Var[RunModel],
                     errors: Var[List[String]],
                     papers: Var[Map[String, Bookmark]],
                     selectedTab: Var[String] = Var("Simulations"),
                     selectedImage: Var[String] = Var("presentation/people.jpg"),
                     selectedPaper: Var[String] = Var("/files/ossilator/Stricker08.pdf")
                   )
{
  import org.denigma.binding.extensions._
  import rx.Ctx.Owner.Unsafe.Unsafe
  selectedImage.onChange {
    case "" =>
    case img => go2images()
  }
  selectedPaper.onChange {
    case "" =>
    case img => go2images()
  }

  def go2images() =     selectedTab() = "Image"
  def go2papers() =     selectedTab() = "Papers"

}