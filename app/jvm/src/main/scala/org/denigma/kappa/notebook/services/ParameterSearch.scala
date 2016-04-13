package org.denigma.kappa.notebook.services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import org.denigma.kappa.messages.{RunModel, SimulationStatus}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

case class ToySearch(model: RunModel, original: SimulationStatus, fit: Fit, makeRuns: Int)
{
}

case class Fit(parameter: String, min: Double, max: Double, runNum: Int)
{
  def generate(code: String) = {

  }

}

class ParameterSearcher(host: String = "localhost", port: Int = 8080)(implicit system: ActorSystem, mat: ActorMaterializer) extends WebSimClient(host, port)(system, mat){

  /*
  def run(models: Seq[WebSim.RunModel]): Future[Seq[(TokenPoolMessage, SimulationStatus)]] =  {
    Source(models).via(defaultRunModelFinalResultFlow).runWith(Sink.seq)
  }
*/
  /*
  def stupidSearchFlow(toySearch: ToySearch) = {
    for{ i <- 1 to 10 }
      yield toySearch
  }
*/

  /*
  def makeSearchFlow(parallelism: Int = defaultParallelism, streamInterval: FiniteDuration = defaultUpdateInterval)= GraphDSL.create()
  {
    implicit builder=>
    import GraphDSL.Implicits._

    val output = Inlet[SimulationStatus]("search.in")
    val input = Outlet[ToySearch]("search.out")

    val runner: FlowShape[RunModel, (TokenPoolMessage, SimulationStatus)] = builder.add(makeModelResultsFlow(parallelism, streamInterval))
    val model = builder.add(Flow[ToySearch].map(toy => toy.model))
    val split = builder.add(Broadcast[ToySearch](2))
    val toy2Model = builder.add(Flow[ToySearch].map(m=>m.model))
    //val toy2Fit = builder.add(Flow[ToySearch].map(m=>m.fit))
    //val zip = builder.add(ZipWith(2))
//      val split = builder.add(UnzipWith.apply[ToySearch, ToySearch, RunModel])


      //split.out(0) ~> toy2Model ~> runner
    //split.out(1) ~> toy2Fit
    input ~> split.in


      //split ~> Flow.map(m=>m.) model.in
    //model.out ~> runner.in
   // runner.out
//      in ~> runner.in
      //val modelToRun = builder.add(runFlow.shape.in)


    //val zip1 = b.add(ZipWith[Int, Int, Int](math.max _))
    //val zip2 = b.add(ZipWith[Int, Int, Int](math.max _))

    //UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
    FlowShape(output, input)
  }

  def stats(model: WebSim.RunModel) = server.run(model) map{
    case status=>
      val km = KappaMatrix(List(status.plot.get))
      //sum()
    val change = model.code
  }
  */
}

/*
case class KappaMatrix(plots: List[KappaPlot])
{
  import breeze.linalg._
  import breeze.numerics._

  import breeze.stats
  import breeze.stats._
  import breeze.stats.distributions._

  require(plots.isEmpty, "Plots should not be an empty list")

  val head = plots.head
  val colLen: Int = head.legend.length
  val rowLen: Int = head.observables.length

  require(plots.forall(p => p.legend sameElements head.legend), "Plots should have the same legend")
  require(plots.forall(p => p.observables.length==head.observables.length), "Plots should have the sale dimensions")
  require(plots.forall(p => p.timePoints==head.timePoints), "TimePoints should be the same")

  lazy val matrix: DenseMatrix[Double] = {
    val vectors =
      for {
        p <- plots
        o <- p.observables.map(o=>o.values)
      } yield DenseVector(o)
    val mat: DenseMatrix[Double] = DenseMatrix(vectors:_*)
    mat
  }

  lazy val rowSum: DenseVector[Double] = sum(matrix(*, ::))
  lazy val colSum = sum(matrix(::, *))
  lazy val rowStd = stats.stddev(matrix(*, ::))
  lazy val colStd = stats.stddev(matrix(::, *))

}
*/