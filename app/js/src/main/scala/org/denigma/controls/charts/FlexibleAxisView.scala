package org.denigma.controls.charts

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, CollectionSeqView, CollectionSortedMapView}
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic

import scala.collection.immutable.SortedMap


/**
  * Created by antonkulaga on 9/27/16.
  */
class FlexibleAxisView(val elem: Element, scale: Rx[Scale], style: Rx[LineStyles])
  extends BindableView with CollectionSortedMapView
{

  override type Key = Int
  override type Value = Tick
  override type ItemView = FlexibleTickView

  val title = scale.map(_.title)
  //val start: rx.Rx[Double] = scale.map(_.start)
  //val end: rx.Rx[Double] = scale.map(_.end)

  val startCoord = scale.map(_.startCoord)
  val endCoord = scale.map(_.endCoord)
  val length = Rx{scale().length}
  //val ticks: Dynamic[List[Double]] = scale.map(_.ticks)
  //val inverted = scale.map(_.inverted)

  val strokeWidth = style.map(_.strokeWidth)
  val strokeColor = style.map(_.strokeColor)
  lazy val tickLength = Var(10.0)
  lazy val half = length.map(_/2)

  override def items: Rx[SortedMap[Int, Tick]] = scale.map{ sc =>
    val list: List[(Int, Tick)] = sc.ticks.zipWithIndex.map{ case (tk, index) => (index, Tick(tk.toString, sc.coord(tk)))}
    SortedMap[Int, Tick](list:_*)
  }

  override def updateView(view: ItemView, key: Int, old: Tick, current: Tick): Unit = {
    view.tick() = current
  }

  override def newItemView(key: Int, value: Tick): ItemView = this.constructItemView(key){
    case (el, _) => new FlexibleTickView(el, Var(value), tickLength, style).withBinder(v=> new GeneralBinder(v))
  }
}

class FlexibleTickView( elem: Element, val tick: Var[Tick], tickLength: Rx[Double], styles: Rx[LineStyles]) extends TickView(elem, tick, tickLength, styles)
