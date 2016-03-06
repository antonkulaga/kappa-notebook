package org.denigma.kappa.notebook.views.editor

import fastparse.all
import org.denigma.binding.binders.ReactiveBinder
import org.denigma.binding.views.BindableView
import org.denigma.codemirror._
import org.denigma.codemirror._
import org.denigma.codemirror.extensions.EditorConfig
import org.scalajs.dom
import org.scalajs.dom.html.Anchor
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import rx.Var
import scala.scalajs.js.JSConverters._
import scala.scalajs.js
import org.denigma.codemirror.extensions._
import scalatags.JsDom.all._
import org.denigma.binding.extensions._
import scalatags.JsDom.all._
import fastparse.all._

class CommentsParser
{
  val comment = P( "##" | "#^" | "#" )
  val notComment = P( ! comment ).flatMap(v => AnyChar)
  val bracketsOrSpace = P("<" | ">" | " ")
  val notBracketsOrSpace = CharPred(ch => ch != '<' && ch != '>' && ch != ' ')
  val protocol = P( ("http" | "ftp" ) ~ "s".? ~ "://" )
  val link: P[String] = P("<".? ~ (protocol ~ notBracketsOrSpace.rep).! ~ ">".? ) //map(_.toString)
  val linkAfterComment: all.Parser[String] = P( notComment.rep  ~ comment ~ " ".rep ~ link )
}

/*case object FeaturesParser extends CommentsParser {
  def parse(str: String) = linkAfterComment.parse(str)
}*/

