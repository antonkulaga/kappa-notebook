package org.denigma.kappa.notebook.views

import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.denigma.controls.login.Session
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx.core._


class NotebookView(val elem: Element, val session: Session) extends BindableView
{
  self =>

  val odes = Var(new CompBioODEs())

  val abc =
    """
      |####### TEMPLATE MODEL AS DESCRIBED IN THE MANUAL#############
      |
      |#### Signatures
      |%agent: A(x,c) # Declaration of agent A
      |%agent: B(x) # Declaration of B
      |%agent: C(x1~u~p,x2~u~p) # Declaration of C with 2 modifiable sites
      |
      |#### Rules
      |'a.b' A(x),B(x) <-> A(x!1),B(x!1) @ 'on_rate','off_rate' #A binds B
      |'ab.c' A(x!_,c),C(x1~u) ->A(x!_,c!2),C(x1~u!2) @ 'on_rate' #AB binds C
      |'mod x1' C(x1~u!1),A(c!1) ->C(x1~p),A(c) @ 'mod_rate' #AB modifies x1
      |'a.c' A(x,c),C(x1~p,x2~u) -> A(x,c!1),C(x1~p,x2~u!1) @ 'on_rate' #A binds C on x2
      |'mod x2' A(x,c!1),C(x1~p,x2~u!1) -> A(x,c),C(x1~p,x2~p) @ 'mod_rate' #A modifies x2
      |
      |#### Variables
      |%var: 'on_rate' 1.0E-4 # per molecule per second
      |%var: 'off_rate' 0.1 # per second
      |%var: 'mod_rate' 1 # per second
      |%obs: 'AB' A(x!x.B)
      |%obs: 'Cuu' C(x1~u?,x2~u?)
      |%obs: 'Cpu' C(x1~p?,x2~u?)
      |%obs: 'Cpp' C(x1~p?,x2~p?)
      |
      |
      |#### Initial conditions
      |%init: 1000 A(),B()
      |%init: 10000 C()
      |
      |%mod: [true] do $FLUX "flux.dot" [true]
      |%mod: [T]>20 do $FLUX "flux.dot" [false]
    """.stripMargin

  val code = Var(abc)

  val send = Var(org.denigma.binding.binders.Events.createMouseEvent)
  send.handler{
    dom.alert("this should work soon!")
  }

   override lazy val injector = defaultInjector
    .register("results")((el, args) => new ResultsView(el).withBinder(n => new ResultsBinder(n)))
}



/*

class CodeCellBinder(view: BindableView, onCtrlEnter: Doc => Unit) extends CodeBinder(view) {

  lazy val ctrlHandler: js.Function1[Doc, Unit] = onCtrlEnter
  //lazy val delHandler:js.Function1[Doc,Unit] = onDel


  override def makeEditor(area: HTMLTextAreaElement, textValue: String, codeMode: String, readOnly: Boolean = false) = {
    val editor = super.makeEditor(area, textValue, codeMode, readOnly)
    val dic = js.Dictionary(
      "Ctrl-Enter" -> ctrlHandler
    )
    editor.setOption("extraKeys", dic)
    editor
  }

}
*/
