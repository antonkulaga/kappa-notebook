package org.denigma.kappa.notebook.views.simulations.snapshots

import fastparse.core.Parsed
import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.Pattern
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.parsers.KappaParser
import org.scalajs.dom.raw.{Element, MouseEvent}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.{Rx, Var}

import scala.concurrent.duration._

/**
  * Snapshot view, used to display the snapshots distributions
  */
class SnapView(val elem: Element, val item: Var[KappaModel.KappaSnapshot], val selected: Var[String]) extends BindableView {

  val fileName = item.map(i=>i.name)

  val event = item.map(i=>i.event)

  val active = Rx{
    selected() == fileName()
  }

  val patternString = Var("")

  val parser = new KappaParser()

  val filterPattern: Var[Pattern] = patternString.mapAfterLastChange[Pattern](800 millis, Pattern.empty){ str => parser.rulePart.parse(str) match {
      case Parsed.Success(pat, _) => pat
      case Parsed.Failure(_, _, extra) => Pattern.empty
    }
  }

  val byLength = Var(false)

  /*
  val filterCaption = byLength.map{
    case true => "Group by:"
    case false => "Filter by:"
  }
  */

  val filterIcon =filterPattern.map{
    case pat if pat.isEmpty => "all"
    case _ => "filtered"
  }


  /*
  val filterClick = Var(Events.createMouseEvent())
  filterClick.onChange{
    ev=>
      val str = patternString.now
      parser.rulePart.parse(str) match {
      case Parsed.Success(pat, _) =>
        filterPattern() = pat
      case Parsed.Failure(_, _, extra) =>
        patternString() = ""
        filterPattern() =  Pattern.empty
    }
  }
  */

  val saveKappaSnapshot: Var[MouseEvent] = Var(Events.createMouseEvent())
  saveKappaSnapshot.triggerLater{
    println("KA CLICK")
    val txt = item.now.patterns.foldLeft(""){
      case (acc, (p , q)) => acc + KappaModel.InitCondition(Right(q), p).toKappaCode + "\n"
    }
    saveAs(fileName.now+".ka", txt)
  }

  val saveCSVSnapshot: Var[MouseEvent] = Var(Events.createMouseEvent())
  saveCSVSnapshot.triggerLater{
    println("TSV CLICK")
    val txt = item.now.patterns.foldLeft(""){
      case (acc, (p , q)) => acc + p.toKappaCode + "\t"+q + "\n"
    }
    saveAs(fileName.now+".tsv", txt)
  }

  override lazy val injector = defaultInjector
    .register("BarPlot") {
      case (el, _) =>
        new BarPlot(el, item, filterPattern, Var(false)).withBinder(new GeneralBinder(_))
    }

}