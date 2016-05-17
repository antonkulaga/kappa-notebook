package org.denigma.kappa.notebook

import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.papers.Bookmark
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.{Rx, Var}

import scala.collection.immutable._
import org.denigma.kappa.messages
import org.denigma.kappa.messages.{KappaPath, _}
import org.scalajs.dom

object Selector {
  lazy val default = Selector(
    Var("Simulations"),
    Var(""),
    Var(""),
    Var(""),//Var("/files/ossilator/Stricker08.pdf"),
    Var("")
  )
}

case class Selector(
                     tab: Var[String],
                     source: Var[String],
                     image: Var[String],
                     paper: Var[String],
                     video: Var[String]
                   )
{
  import org.denigma.binding.extensions._
  import rx.Ctx.Owner.Unsafe.Unsafe

  def go2simulations() = tab() = "Simulations"


  def go2images() = tab() = "Images"

  def go2papers() = tab() = "Papers"

  def go2videos() = tab() = "Videos"

  /*
  image.onChange {
    case "" =>
    case img => go2images()
  }

  paper.onChange {
    case "" =>
    case img => go2images()
  }
  */

}

/**
  * Created by antonkulaga on 07/04/16.
  */
