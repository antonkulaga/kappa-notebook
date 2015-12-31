package org.denigma.kappa.notebook.views
import org.denigma.binding.views._
import org.denigma.kappa.messages.KappaMessages
import org.scalajs.dom.raw.Element
import rx.core._
import rx.ops._
import org.denigma.binding.extensions._

class RunnerView(val elem: Element, val parameters: Var[KappaMessages.RunParameters]) extends BindableView
{
  self=>

  def opt(n: Int) = if(n>0) Some(n) else None

  val events: Var[Int] = Var(10000)
  var time: Var[Int] = Var(0)
  val points: Var[Int] = Var(250)

  val output = Rx{
    val ev = self.events()
    val t = self.time()
    val p = self.points()
    val params = parameters.now
    parameters.set(params.copy(events = opt(ev), time = opt(t), points = p))
  }

}
