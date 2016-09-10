package org.denigma.kappa.notebook.views.annotations
import org.denigma.binding.binders.Events
import org.denigma.kappa.notebook.views.editor.{EmptyCursor, KappaCursor, KappaEditorCursor}
import org.scalajs.dom
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe


trait CommentInserter {

  def kappaCursor: Var[KappaCursor]

  lazy val additionalComment = Var("")

  protected def toURI(str: String) = str.replace(" ", "%20") match {
    case s if s.contains(":") => s
    case other => ":" + other
  }

  def comment: Rx[String]

  def hasComment: Rx[Boolean]

  lazy val line: Rx[String] = kappaCursor.map{
    case KappaEditorCursor(file, ed, l, ch) => l +":" + ch
    case _ => ""
  }

  lazy val lineNumber: Rx[Int] = kappaCursor.map{ c => c.lineNum}
  lazy val charNumber: Rx[Int] = kappaCursor.map{c => c.ch}

  lazy val canInsert: Rx[Boolean] = kappaCursor.map(c => c != EmptyCursor)

  lazy val codeFile = kappaCursor.map{
    case KappaEditorCursor(file, ed, l, ch) =>  file.path
    case _ => ""
  }

  lazy val insertComment = Var(Events.createMouseEvent())
  insertComment.triggerLater{
    kappaCursor.now match {
      case c @ KappaEditorCursor(file, ed, lineNum, ch) =>
        val doc = ed.getDoc()
        val line = doc.getLine(lineNum)
        doc.replaceRange(comment.now, c.position, c.position)

      case _ => dom.console.error("EDITOR IS NOT AVALIABLE TO INSERT")
    }
  }

}
