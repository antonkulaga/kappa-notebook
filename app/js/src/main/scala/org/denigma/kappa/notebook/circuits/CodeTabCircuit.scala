package org.denigma.kappa.notebook.circuits
/*
import org.denigma.codemirror.Editor
import org.denigma.kappa.messages.{Go, KappaMessage}
import org.scalatest.path
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.scalajs.js

/**
  * Created by antonkulaga on 9/22/16.
  */
class CodeTabCircuit(input: Var[KappaMessage], output: Var[KappaMessage], getEditor: ()=> Editor) extends Circuit(input, output) {

  def editor = getEditor()

  override protected def onInputMessage(message: KappaMessage): Unit = message match {
    case Go.ToSource(p, from ,to) if p.value == path.now | p.local ==path.now | p.local == name.now | (p.value == "" && active.now) =>
      println(s"from ${from} to ${to}")
      editor.getDoc().setCursor(js.Dynamic.literal(line = from, ch = 1).asInstanceOf[Position])

    case Go.ToSource(p, from ,to) if p.local == name.now | (p.value == "" && active.now) =>
      println(s"from ${from} to ${to}")
      editor.getDoc().setCursor(js.Dynamic.literal(line = from, ch = 1).asInstanceOf[Position])

    case _=> //do nothing
  }

}
*/