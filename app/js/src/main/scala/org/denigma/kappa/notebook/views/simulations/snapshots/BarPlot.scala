package org.denigma.kappa.notebook.views.simulations.snapshots
import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.denigma.controls.charts._
import org.denigma.controls.drawing.Rectangle
import org.denigma.kappa.model.KappaModel.{KappaSnapshot, Pattern}
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx.{Rx, Var}

import scala.collection.immutable.Iterable

/**
  * Created by antonkulaga on 10/21/16.
  */
object BarPlot {

    protected def defaultWidth: Double = 1000

    protected def defaultHeight: Double = 800

}

/**
  * Bar plot for snapshots
 *
  * @param elem Element
  * @param snapshot snapshot
  * @param filterPattern pattern that is used to filter or group snapshots by some pattern
  * @param byLength if we should group by length
  */
class BarPlot(val elem: Element,
              val snapshot: Rx[KappaSnapshot],
              val filterPattern: Rx[Pattern],
              val byLength: Rx[Boolean]
               ) extends CollectionSeqView with Plot {

  type Item = Bar

  type ItemView = BarView

  lazy val label = Var("") //label of the plot, is also used to show value on which the pointer is

  lazy val paddingX = Var(50.0)

  lazy val paddingY = Var(50.0)

  lazy val viewBox: Dynamic[String] = Rx{
    s"${0} ${0} ${width() + paddingX() * 2  } ${height() + paddingY() * 2 }"
  }

  val halfWidth = Rx{ width() / 2.0 }

  val chartStyles: Rx[ChartStyles] = Var(ChartStyles.default)

  val title = scaleX.map(sc=> sc.title)


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

  /**
    * Patterns from the snapshots
    */
  lazy val patterns: Rx[Map[Pattern, Int]] = Rx{
    val snap = snapshot()
    val pat = filterPattern()
    if(pat.isEmpty) snap.patterns else snap.embeddingsOf(pat)
  }

  val items = Rx{
    val pts = patterns()
    val byLen = byLength()
    val resultIterator: Iterable[Bar] = if(byLen) {
      pts
        .groupBy{ case (pat, q) => pat.agents.length}
        .map{ case (len, mp) => PatternGroupBar(mp)}
    } else pts.map{ case (pat, q) => PatternBar(pat, q)}
    resultIterator.toList.sorted
  }

  lazy val scaleX = Rx{
    val pats = patterns()
    val len = pats.size
    val w = BarPlot.defaultWidth
    val byLen = byLength()
    //val stepSize = Math.min(pats.size , 20)
    //println("stepSize = "+stepSize)
    val title = if(byLen) "sizes" else "patterns"
    //LinearScale(title, 0, len, step, w)
    //TODO: fix linear scale error
    val sc = SimpleFlexibleLinearScale(title, 0, pats.size, 1, w)
    //println(s"STEPSIZE(${sc.stepSize}) LENGTH ${sc.length}")
    sc
  }


  lazy val scaleY: Rx[SimpleFlexibleLinearScale] =  Rx{
    val pats = patterns()
    val end = pats.values.max
    val h = BarPlot.defaultHeight
    val stepSize = Math.min(end , 20)
    SimpleFlexibleLinearScale("pattern lengths", 0, end , stepSize ,h , inverted = true)
    //LinearScale("pattern lengths", 0, len, step, h)
  }

  lazy val position: Rx[Bar => Rectangle] = Rx{
    val scX = scaleX()
    val scY = scaleY()
    val its = items()
    val result: Bar => Rectangle = {bar =>
        val i = its.indexOf(bar)
        val x = scX.coord(i)
        //println("X = "+x)
        val y = scY.coord(bar.quantity, true)
        val height: Double =  scY.coord(bar.quantity, false)
        val width = scX.coord(i, false)
        Rectangle(x, y, width, height)
    }
    result
  }

  override def newItemView(item: Bar): BarView = this.constructItemView(item){
    case (el, _) => item match {
      case p @ PatternGroupBar(data) => new PatternGroupBarView(el, p, position, label).withBinder(v=> new GeneralBinder(v))
      case p @ PatternBar(pat, q) => new PatternBarView(el, p, position, label).withBinder(v=> new GeneralBinder(v))
    }
  }

  override lazy val injector = defaultInjector
    .register("ox"){case (el, args) => new AxisView(el, scaleX, chartStyles.map(_.scaleX))
      .withBinder(new GeneralBinder(_))}
    .register("oy"){case (el, args) => new AxisView(el, scaleY, chartStyles.map(_.scaleY))
      .withBinder(new GeneralBinder(_))}
}

/**
  * View for grouped bar patterns
  * @param elem Element to bind to
  * @param item pattern group
  * @param position the function that positions the bar
  * @param label we change label depending
  */
class PatternGroupBarView(val elem: Element, val item: PatternGroupBar, position: Rx[Bar => Rectangle],  label: Var[String]) extends BarView {

  val rectangle = position.map(pos => pos(item))

  val x = rectangle.map(rect=>rect.x)
  val y = rectangle.map(rect=>rect.y)
  val width = rectangle.map(rect=>rect.width)
  val height = rectangle.map(rect=>rect.height)

  val onMouseEnter = Var(Events.createMouseEvent())
  onMouseEnter.onChange{ev=>
    label() = item.name
  }

  val onMouseExit = Var(Events.createMouseEvent())
  onMouseExit.onChange{ev=>
    //label() = ""
  }

  val quantity: Rx[Double] = Var(item.quantity)
}

/**
  * A view for individual pattern
  * @param elem
  * @param item
  * @param position
  * @param label
  */
class PatternBarView(val elem: Element, val item: PatternBar, position: Rx[Bar => Rectangle], val label: Var[String] ) extends BarView {

  val rectangle = position.map(pos => pos(item))

  val x = rectangle.map(rect=>rect.x)
  val y = rectangle.map(rect=>rect.y)
  val width = rectangle.map(rect=>rect.width)
  val height = rectangle.map(rect=>rect.height)

  val onMouseEnter = Var(Events.createMouseEvent())
  onMouseEnter.onChange{ev=>
    label() = item.name
  }

  val onMouseExit = Var(Events.createMouseEvent())
  onMouseExit.onChange{ev=>
    label() = ""
  }

  val quantity: Rx[Double] = Var(item.quantity)
}

trait BarView extends BindableView{

  def item: Bar

  def width: Rx[Double]
  def height: Rx[Double]
  def x: Rx[Double]
  def y: Rx[Double]
  def quantity: Rx[Double]

}
