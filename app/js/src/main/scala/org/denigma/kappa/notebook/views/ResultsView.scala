package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{Events, GeneralBinder, ReactiveBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.macroses._
import org.denigma.binding.views._
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.login.Session
import org.denigma.controls.tabs._
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.Element
import rx.core._

import scala.collection.immutable._


import rx.ops._

trait TabItemView2 extends BindableView {

  val item: Rx[TabItem]
  val selection: Var[Option[Rx[TabItem]]]

  val content: rx.Rx[String] = item.map(_.content)
  val label: rx.Rx[String] = item.map(_.label)

  lazy val active: Rx[Boolean] = Rx{
    val sel = selection()
    sel.isDefined && sel.get.now == item()
  }

  val onClick = Var(Events.createMouseEvent())
  onClick.handler{
    selection() = Some(this.item)
  }
}

/**
  * Created by antonkulaga on 12/5/15.
  */
class ResultsView(val elem: Element) extends BindableView with InitialConditions{

  self =>

  private val console = Var(TabItem("Console", "Console content"))
  private val chart = Var(TabItem("Chart", "Chart content"))
  private val causality = Var(TabItem("Causality", "Causality content"))

  val items:Var[Seq[Var[TabItem]]] = Var(
    Seq( console, chart, causality )
  )

  lazy val isConsole = Rx{
    val a = active()
    a.isDefined && a.get.now.label.toLowerCase == "console"
  }

  lazy val isChart = Rx{
    val a = active()
    a.isDefined && a.get.now.label.toLowerCase == "chart"
  }


  lazy val isCausality = Rx{
    val a = active()
    a.isDefined && a.get.now.label.toLowerCase == "causality"
  }

  protected def defaultContent = ""
  protected def defaultLabel = ""

  type Item = Rx[TabItem]

  val active: Var[Option[Item]] = Var(Some(console))

  val selected: Var[String] = Var("console")
  selected.onChange("activeTabChanged", uniqueValue = true){
    case value =>
      val lv  = value.toLowerCase
      val sel = items.now.collectFirst{ case tb if tb.now.label.toLowerCase == lv =>tb}
      this.active() = sel
  }

  override lazy val injector = defaultInjector
    .register("Chart") {
      case (el, params) =>
        new ProteinsTime(el, Var(new CompBioODEs()), initialConditions, chart, active
        ).withBinder(new GeneralBinder(_, self.binders.collectFirst { case r: ReactiveBinder => r }))
    }
    .register("Console") {
      case (el, params) =>
        val v = new ConsoleView(el, console, active).withBinder(new CodeBinder(_))
        val b = v.binders.collectFirst{ case b: CodeBinder[_]=>b}
        println("STRINGS KEYS "+b.get.allStringsKeys)
        println(s"BOOLS [${b.get.bools.keySet.toList.mkString(" ")}]")
        v
    }
}



class ConsoleView(val elem: Element, val item: Rx[TabItem], val selection: Var[Option[Rx[TabItem]]]) extends TabItemView2{

  val output =
    """
      |+ Self seeding...
      |+ Initialized random number generator with seed 96373529
      |+ Building initial simulation conditions...
      |+ Compiling...
      |	 -simulation parameters
      |	 -agent signatures
      |+ Compute the contact map
      |	 -variable declarations
      |	 -rules
      |	 -observables
      |	 -perturbations
      |	 -initial conditions
      |	 Done
      |File "abc.ka", line 19, characters 6-19:
      |Warning: Deprecated variable syntax: use |kappa instance| instead.
      |File "abc.ka", line 20, characters 6-26:
      |Warning: Deprecated variable syntax: use |kappa instance| instead.
      |File "abc.ka", line 21, characters 6-26:
      |Warning: Deprecated variable syntax: use |kappa instance| instead.
      |File "abc.ka", line 22, characters 6-26:
      |Warning: Deprecated variable syntax: use |kappa instance| instead.
      |____________________________________________________________
      |############################################################
      |Simulation ended
    """.stripMargin

  val console = Var(output)
}
