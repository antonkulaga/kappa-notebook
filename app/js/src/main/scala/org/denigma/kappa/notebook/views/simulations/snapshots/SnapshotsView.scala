package org.denigma.kappa.notebook.views.simulations.snapshots

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.CollectionSortedMapView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.model.KappaModel.KappaSnapshot
import org.denigma.kappa.notebook.views.common.FileTabHeaders
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable.SortedMap

/**
  * View for snapshots
  * @param elem HTML element to bind to
  * @param snapshots snapshots received from the simulations
  * @param input
  * @param selected what time was selected
  */
class SnapshotsView(val elem: Element,
                    val snapshots: Rx[List[KappaSnapshot]],
                    val input: Var[KappaMessage],
                    val selected: Var[String])
    extends CollectionSortedMapView {

  override type Key = (String, Int)
  override type Value = KappaSnapshot
  override type ItemView = SnapView

  implicit val ordering = new Ordering[(String, Int)]{
    override def compare(x: (String, Int), y: (String, Int)): Int = (x, y) match {
      case ((nameX, eventX), (nameY, eventY)) if  eventX.compare(eventY) == 0 => nameX.compare(nameY)
      case ((nameX, eventX), (nameY, eventY)) => eventX.compare(eventY)
    }
  }

  val items = snapshots.map(snaps => SortedMap.apply(snaps.map(s=> ((s.name, s.event) , s)):_*))

  val active: Rx[Boolean] = selected.map(s=>s=="snapshots")

  val subtab: Var[String] = Var("")

  val headers = items.map(its => its.keySet.map{ case (name, event) => name}.toList)

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new FileTabHeaders(el, headers, input, subtab)(FileTabHeaders.path2name).withBinder(new GeneralBinder(_)))

  override def updateView(view: SnapView, key: (String, Int), old: KappaSnapshot, current: KappaSnapshot): Unit = {
    view.item() = current
  }

  override def newItemView(key: (String, Int), value: KappaSnapshot): SnapView = this.constructItemView(key){
    case (el, _) =>
      val view = new SnapView(el, Var(value), subtab).withBinder(v => new CodeBinder(v))
      subtab() = value.name
      view
  }
}
