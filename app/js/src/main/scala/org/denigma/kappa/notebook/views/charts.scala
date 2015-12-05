package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.controls.charts._
import org.denigma.controls.tabs.TabItem
import org.scalajs.dom.raw.Element
import rx.core.{Var, Rx}
import rx.ops._
import scala.collection.immutable._

/**
  * Created by antonkulaga on 11/16/15.
  */
trait InitialConditions {
  lazy val lacI_mRNA_start = Var(0.0)
  lazy val tetR_mRNA_start = Var(0.0)
  lazy val lacI_start = Var(0.0)
  lazy val tetR_start = Var(0.0)
  lazy val initialConditions = Rx{ Array(lacI_mRNA_start(), tetR_mRNA_start(), lacI_start(), tetR_start()) }
}

class ProteinsTime(val elem: Element,
                   val odes: Rx[CompBioODEs],
                   val initialConditions: Rx[Array[Double]],
                   val item: Rx[TabItem],
                   val selection: Var[Option[Rx[TabItem]]]) extends LinesPlot with TabItemView2{

  val scaleX: rx.Var[Scale] = Var(LinearScale("Time", 0.0, 5000, 600, 400))
  val scaleY: rx.Var[Scale] = Var(LinearScale("Concentration", 0.0, 2000, 400, 400, inverted = true))
  val coords = odes.now.computeAll(initialConditions.now, 2, 3)

  lazy val lacI_Prod = odes.map(o => o.lacIProduction.production)
  lazy val tetR_Prod = odes.map(o => o.tetRProduction.production)
  lazy val lacI_Delusion = odes.map(o => o.lacIProduction.production)
  lazy val tetR_Delusion = odes.map(o => o.tetRProduction.production)

  val lacI_mRNA = Var(new StaticSeries("LacI mRNA", List.empty).withStrokeColor("pink"))
  val tetR_mRNA = Var(new StaticSeries("TetR mRNA", List.empty).withStrokeColor("cyan"))
  val lacI = Var(new StaticSeries("LacI", List.empty).withStrokeColor("red"))
  val tetR = Var(new StaticSeries("TetR", List.empty).withStrokeColor("blue"))

  lazy val solve = Var(Events.createMouseEvent)

  protected def onSolve() = {
    val coords = odes.now.computeAll(initialConditions.now, 2, 3)
    require(coords.length > 3, "odes should include 4 elements")
    lacI_mRNA() = lacI_mRNA.now.copy(points = coords(0).toList)
    tetR_mRNA() = tetR_mRNA.now.copy(points = coords(1).toList)
    lacI() = lacI.now.copy(points = coords(2).toList)
    tetR() = tetR.now.copy(points = coords(3).toList)
  }

  solve.handler{
    onSolve()
  }

  override def newItemView(item: Item): SeriesView = constructItemView(item){
    case (el, mp) => new SeriesView(el, item, transform).withBinder(new GeneralBinder(_))
  }

  val items: Var[Seq[Item]] = Var(Seq(lacI_mRNA, tetR_mRNA, lacI, tetR))
}

import org.denigma.controls.charts.ode.ODEs

case class Reactant(concentration:Double)

case class HillRepression(kProd: Double, kRepress: Double, nRepressor: Double, delusion: Double, leakage: Double) {

  def repress(mRNA:Double, repressor: Double): Double = {
    //if (value.isNaN) throw new Exception(s"IS NAN: with product $product and repressor $repressor and dis Repressor $k_disRepressor")
    //if (value.isInfinite) throw new Exception(s"IS INFINIT: with product $product and repressor $repressor and dis Repressor $k_disRepressor")
    val result = kProd / (1 + Math.pow( (repressor / kRepress), nRepressor) ) - delusion * mRNA + leakage
    result
  }
}

case class ProductionDelusion(production: Double, delusion: Double){

  def apply(x: Double, y: Double): Double = production * x - y * delusion
  def apply(x: Double): Double = apply(x, x)

}

object Defaults1 {


  lazy val gamma_L_m: Double = 0.04
  lazy val gamma_T_m: Double = 0.04
  lazy val kappa_L_m0: Double = 0.0082
  lazy val kappa_T_m0: Double = 0.0149
  lazy val kappa_L_m: Double = 1
  lazy val kappa_T_m: Double = 0.3865

  lazy val gamma_L_p: Double = 0.002
  lazy val gamma_T_p: Double = 0.002
  lazy val kappa_L_p: Double = 0.1
  lazy val kappa_T_p: Double = 0.2
  lazy val theta_L: Double = 600.0
  lazy val theta_T: Double = 500.0
  lazy val eta_L: Double = 4
  lazy val eta_T: Double = 4

  lazy val lacIRepression = HillRepression(kappa_L_m, theta_T, eta_T,gamma_L_m, kappa_L_m0)
  lazy val tetRRepression = HillRepression(kappa_T_m, theta_L, eta_L,gamma_T_m, kappa_T_m0)
  lazy val lacIProduction = ProductionDelusion(kappa_L_p, gamma_L_p)
  lazy val tetRProduction = ProductionDelusion(kappa_T_p, gamma_T_p)
}

object Defaults {

  lazy val gamma_L_m: Double = Math.log(2)/3
  lazy val gamma_T_m: Double = Math.log(2)/3
  lazy val kappa_L_m0: Double = 0.0
  lazy val kappa_T_m0: Double = 0.0
  lazy val kappa_L_m: Double = 20 * gamma_L_m
  lazy val kappa_T_m: Double = 20 * gamma_T_m
  lazy val gamma_L_p: Double = Math.log(2)/20
  lazy val gamma_T_p: Double = Math.log(2)/20
  lazy val kappa_L_p: Double = 2000 * gamma_L_p / 20
  lazy val kappa_T_p: Double = 2000 * gamma_T_p / 20
  lazy val theta_L: Double = 1000.0
  lazy val theta_T: Double = 1000.0
  lazy val eta_L: Double = 2.5
  lazy val eta_T: Double = 2.5

  lazy val lacIRepression = HillRepression(kappa_L_m, kappa_T_m, eta_T, gamma_L_m, kappa_L_m0)
  lazy val tetRRepression = HillRepression(kappa_T_m, kappa_L_m, eta_L, gamma_T_m, kappa_T_m0)
  lazy val lacIProduction = ProductionDelusion(kappa_L_p, gamma_L_m)
  lazy val tetRProduction = ProductionDelusion(kappa_T_p, gamma_T_m)

}

case class CompBioODEs(
                        lacRepressedByTetR: HillRepression = Defaults1.lacIRepression,
                        tetRepressedByLacI: HillRepression = Defaults1.tetRRepression,
                        lacIProduction: ProductionDelusion = Defaults1.lacIProduction,
                        tetRProduction: ProductionDelusion = Defaults1.tetRProduction,
                        tEnd: Double = 5000, override val step: Double = 1) extends ODEs
{

  override val tStart = 0.0

  def d_LacI_mRNA (t: Double, p: Array[Double]): Double =  lacRepressedByTetR.repress(p(0), p(3))
  def d_TetR_mRNA (t: Double, p: Array[Double]): Double = tetRepressedByLacI.repress(p(1), p(2))
  def d_LacI (t: Double, p: Array[Double]): Double = lacIProduction(p(0), p(2))
  def d_TetR (t: Double, p: Array[Double]): Double = tetRProduction(p(1), p(3))

  lazy val derivatives: Array[VectorDerivative] = Array(d_LacI_mRNA, d_TetR_mRNA, d_LacI, d_TetR)

  def solve(lacI_mRNA: Double, tetR_mRNA: Double, lacI: Double, tetR: Double) = {
    val result = this.compute(Array(lacI_mRNA: Double, tetR_mRNA: Double, lacI: Double, tetR: Double))
    result
  }

}