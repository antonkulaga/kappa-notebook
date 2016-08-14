package org.denigma.kappa.notebook.views.editor

import org.denigma.codemirror.{Editor, Position, PositionLike}
import org.denigma.kappa.messages.KappaSourceFile

sealed trait KappaCursor{
  self =>

  def lineNum: Int
  def ch: Int

  lazy val position: Position =  new PositionLike{
    override val ch: Int = self.ch
    override val line: Int = self.lineNum
  }.asInstanceOf[Position]
}

case object EmptyCursor extends KappaCursor {
  lazy val lineNum = 0
  lazy val ch = 0
}
case class KappaEditorCursor(source: KappaSourceFile, editor: Editor, lineNum: Int = 0, ch: Int = 0 ) extends KappaCursor