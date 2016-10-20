package org.denigma.kappa.notebook.views.simulations.snapshots

import fastparse.core.Parsed
import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.views.{BindableView, CollectionSortedMapView, CollectionSortedSetView, CollectionView}
import org.denigma.kappa.messages.WebSimMessages.Snapshot
import org.denigma.kappa.notebook.views.common.TabItem
import org.scalajs.dom.raw.{Element, MouseEvent}
import rx.{Rx, Var}
import org.denigma.binding.extensions._
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.{KappaSnapshot, Pattern}
import org.denigma.kappa.parsers.KappaParser
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.kappa.notebook.extensions._
import org.denigma.binding.extensions._
import org.denigma.controls.charts.{FlexibleLinearScale, LinearScale, Plot, Scale}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.views.simulations.ChartView
import rx.Rx.Dynamic

import scala.collection.immutable.{Iterable, SortedSet}
import scala.concurrent.duration._

/**
  * Snapshot view, used to display the snapshots distributions
  */
class SnapView(val elem: Element, val item: Var[KappaModel.KappaSnapshot], val selected: Var[String]) extends BindableView with TabItem{

  val fileName = item.map(i=>i.name)

  val event = item.map(i=>i.event)

  val patternString = Var("")

  lazy val parser = new KappaParser()

  val pattern = patternString.mapAfterLastChange[Pattern](400 millis, Pattern.empty){ str => parser.rulePart.parse(str) match {
      case Parsed.Success(pat, _) => pat
      case Parsed.Failure(_, _, extra) => Pattern.empty
    }
  }

  val saveSnapshot: Var[MouseEvent] = Var(Events.createMouseEvent())
  saveSnapshot.triggerLater{
    val txt = item.now.patterns.foldLeft(""){
      case (acc, (p , q)) => acc + KappaModel.InitCondition(Right(q), p).toKappaCode + "\n"
    }

    saveAs(fileName.now+".csv", txt)
  }

  val distribution = pattern

  override lazy val injector = defaultInjector
    .register("Plot") {
      case (el, _) =>
        new BarPlot(el, item, pattern, Var(false)).withBinder(new GeneralBinder(_))
    }

}

object BarPlot {

    protected def defaultWidth: Double = 1000

    protected def defaultHeight: Double = 1000

}

/**
  * Bar plot for snapshots
  * @param elem Element
  * @param title title
  * @param snapshot snapshot
  * @param filterPattern
  * @param byLength
  * @param plotWidth
  * @param plotHeight
  */
class BarPlot(val elem: Element, val title: Rx[String],
              val snapshot: Rx[KappaSnapshot],
              val filterPattern: Rx[Pattern],
              val byLength: Rx[Boolean],
              val plotWidth: Rx[Double] = Var(BarPlot.defaultWidth),
              val plotHeight: Rx[Double] = Var(BarPlot.defaultHeight)
               ) extends CollectionSortedSetView with Plot {

  type Item = Bar

  type ItemView = BarView

  implicit val ordering = new Ordering[Bar]{
    override def compare(x: Item, y: Item): Int = (x, y) match {
      case (x: PatternBar, y: PatternBar) => x.quantity.compare(y.quantity) match {
        case 0 => x.name.compare(y.name)
        case other => other
      }

      case (x: Bar, y: Bar) =>  x.quantity.compare(y.quantity) match {
        case 0 => x.name.compare(y.name)
        case other => other
      }
    }
  }


  val patterns: Rx[Map[Pattern, Int]] = Rx{
    val snap = snapshot()
    val pat = filterPattern()
    if(pat.isEmpty) snap.patterns else snap.embeddingsOf(pat)
  }

  val items: Rx[SortedSet[Item]] = Rx{
    val pts = patterns()
    val byLen = byLength()
    val resultIterator: Iterable[Bar] = if(byLen) {
      pts
        .groupBy{ case (pat, q) => pat.agents.length}
        .map{ case (len, mp) => PatternGroupBar(mp)}
    } else pts.map{ case (pat, q) => PatternBar(pat, q)}
    SortedSet(resultIterator.toList:_*)
  }

  val scaleX: Rx[Scale] = Rx{
    val pats = patterns()
    val len = pats.size
    val w = plotWidth()
    val byLen = byLength()
    val step = Math.round(w / len)
    val title = if(byLen) "sizes" else "patterns"
    LinearScale(title, 0, len, step, w)
  }

  val scaleY: Rx[Scale] =  Rx{
    val pats = patterns()
    val len = pats.values.max
    val h = plotHeight()
    val step = Math.round(h / len)
    LinearScale("pattern lengths", 0, len, step, h)
  }


  val paddingX = Var(50.0)

  val paddingY = Var(50.0)

  override def newItemView(item: Bar): BarView = this.constructItemView(item){
    case (el, _) => item match {
      case p @ PatternGroupBar(data) => new PatternGroupBarView(el, p).withBinder(v=> new GeneralBinder(v))
      case p @ PatternBar(pat, q) => new PatternBarView(el, p).withBinder(v=> new GeneralBinder(v))
    }
  }
}

case class PatternGroupBar(data: Map[Pattern, Int]) extends Bar {
  type Data = Map[Pattern, Int]

  lazy val quantity: Double = data.foldLeft(0){ case (acc, (pat, q)) =>  acc + q}

  lazy val name = quantity.toString
}

case class PatternBar(data: Pattern, quantity: Double) extends Bar {
  type Data = Pattern

  def name = data.toKappaCode

}

trait Bar{
  type Data
  def data: Data
  def quantity: Double
  def name: String

}

class PatternGroupBarView(val elem: Element, val item: PatternGroupBar) extends BarView {
  
}
class PatternBarView(val elem: Element, val item: PatternBar) extends BarView {

}
trait BarView extends BindableView{

  def item: Bar
}
