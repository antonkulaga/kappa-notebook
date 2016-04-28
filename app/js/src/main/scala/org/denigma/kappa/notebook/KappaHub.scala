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
    Var(Loaded.empty),
    Var(SortedSet.empty),
    Var(None),
    Var(messages.Defaults.simulations),
    Var(messages.Defaults.runModel),
    Var(List.empty[String]),
    Var(testMap),
    Selector.default
  )
}

object Selector {
  lazy val default = Selector(
    Var(""), Var("Simulations"), Var("presentation/people.jpg"),  Var("/files/ossilator/Stricker08.pdf")
  )
}
case class Selector(
                     source: Var[String],
                     tab: Var[String],
                     image: Var[String],
                     paper: Var[String])
{
  import org.denigma.binding.extensions._
  import rx.Ctx.Owner.Unsafe.Unsafe

  def go2images() = tab() = "Image"

  def go2papers() = tab() = "Papers"

  image.onChange {
    case "" =>
    case img => go2images()
  }

  paper.onChange {
    case "" =>
    case img => go2images()
  }

}
/**
  * Created by antonkulaga on 07/04/16.
  */
case class KappaHub(
                     loaded: Var[Loaded],
                     sources: Var[SortedSet[KappaFile]],
                     kappaCursor: Var[Option[(Editor, PositionLike)]],
                     simulations: Var[Map[(Int, RunModel), SimulationStatus]],
                     runParameters: Var[RunModel],
                     errors: Var[List[String]],
                     papers: Var[Map[String, Bookmark]],
                     selector: Selector
                   )
{
  import rx.Ctx.Owner.Unsafe.Unsafe

  lazy val name = loaded.map(l=>l.project.name)

  lazy val projects = loaded.map(l=>l.other)

}