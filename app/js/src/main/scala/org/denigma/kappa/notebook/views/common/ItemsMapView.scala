package org.denigma.kappa.notebook.views.common


import org.denigma.binding.extensions._
import org.denigma.binding.views.{BasicView, CollectionView}
import org.denigma.kappa.notebook.MapUpdate
import rx._

import scala.collection.immutable._

trait UpdatableView[Value] extends BasicView
{
  def update(value: Value): this.type

  //def dirty: Rx[Boolean]
}

trait ItemsMapView extends CollectionView{

  import org.denigma.kappa.notebook.extensions._

  type Value

  type Key = Item

  override type ItemView <: UpdatableView[Value]

  def items: Rx[Map[Key, Value]]


  lazy val updates: Rx[MapUpdate[Key, Value]] = items.updates

  override protected def subscribeUpdates() = {
    template.hide()
    //this.items.now.foreach(i => this.addItemView(i, this.newItemView(i)) ) //initialization of views
    updates.onChange(upd=>{
      upd.added.foreach{
        case (key, value)=>
          val n = newItemView(key)
          n.update(value)
          this.addItemView(key, n)
      }
      upd.removed.foreach{ case (key, value ) => removeItemView(key)}
      upd.updated.foreach{ case( key, (old, current))=> itemViews(key).update(current)}
    })
  }

}