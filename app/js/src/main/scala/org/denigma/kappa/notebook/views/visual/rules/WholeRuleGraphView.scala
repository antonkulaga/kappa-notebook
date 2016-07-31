package org.denigma.kappa.notebook.views.visual.rules


import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.kappa.model.KappaModel.Agent
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.layouts._
import org.scalajs.dom
import org.scalajs.dom.raw.{ClientRect, Element, HTMLElement}
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.collection.immutable._

class WholeRuleGraphView(val elem: Element,
                         val unchanged: Rx[Set[Agent]],
                         val removed: Rx[Set[Agent]],
                         val added: Rx[Set[Agent]],
                         val updated: Rx[Set[Agent]],
                         val containerName: String,
                         val visualSettings: RulesVisualSettings
                        ) extends RulesGraphView {

  lazy val size: (Double, Double) = elem.getBoundingClientRect() match {
    case rect: ClientRect if rect.width < 100 || rect.height < 100 => (400.0, 400.0)
    case rect: ClientRect => (rect.width, rect.height)
  }
  val width: Var[Double] = Var(size._1)

  val height: Var[Double] = Var(size._2)

  override lazy val container: HTMLElement = sq.byId(containerName).get
  
  val viz = new Visualizer(container,
    width,
    height,
    layouts,
    750.0,
    iterationsPerFrame,
    firstFrameIterations
  )
}