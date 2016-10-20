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
class SnapView(val elem: Element, val item: Var[KappaModel.KappaSnapshot], val selected: Var[String]) extends BindableView {

  val fileName = item.map(i=>i.name)

  val event = item.map(i=>i.event)

  val active = Rx{
    selected() == fileName()
  }

  val patternString = Var("")

  val parser = new KappaParser()

  val filterPattern: Var[Pattern] = patternString.mapAfterLastChange[Pattern](400 millis, Pattern.empty){ str => parser.rulePart.parse(str) match {
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

  override lazy val injector = defaultInjector
    .register("BarPlot") {
      case (el, _) =>
        new BarPlot(el, item, filterPattern, Var(false)).withBinder(new GeneralBinder(_))
    }

}

object BarPlot {

    protected def defaultWidth: Double = 1000

    protected def defaultHeight: Double = 1000

}

/**
  * Bar plot for snapshots
  * @param elem Element
  * @param snapshot snapshot
  * @param filterPattern pattern that is used to filter or group snapshots by some pattern
  * @param byLength if we should group by length
  */
class BarPlot(val elem: Element,
              val snapshot: Rx[KappaSnapshot],
              val filterPattern: Rx[Pattern],
              val byLength: Rx[Boolean]
               ) extends CollectionSortedSetView with Plot {

  type Item = Bar

  type ItemView = BarView

  lazy val paddingX = Var(50.0)

  lazy val paddingY = Var(50.0)

  lazy val viewBox: Dynamic[String] = Rx{
    s"${0} ${0} ${width() - paddingX() * 2  } ${height() - paddingY() * 2 }"
  }

  val halfWidth = Rx{ width() / 2.0 }

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

  lazy val scaleX: Rx[Scale] = Rx{
    val pats = patterns()
    val len = pats.size
    val w = BarPlot.defaultWidth
    val byLen = byLength()
    val step = Math.round(w / len)
    val title = if(byLen) "sizes" else "patterns"
    LinearScale(title, 0, len, step, w)
  }

  lazy val scaleY: Rx[Scale] =  Rx{
    val pats = patterns()
    val len = pats.values.max
    val h = BarPlot.defaultHeight
    val step = Math.round(h / len)
    LinearScale("pattern lengths", 0, len, step, h)
  }


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
