package org.denigma.kappa

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.stage._

//NOTE: sole IDEs like Intellij think that import is unused while it is used

object extensions {

  implicit class FlowOpsExt[T, Inp, M](val flow: Flow[Inp, T, M]) {

    def upTo(fun: T => Boolean): Flow[Inp, T, M] = flow.via(FlowUpTo(fun))

    def upToExcl(fun: T => Boolean): Flow[Inp, T, M] = flow via FlowUpTo(fun, inclusive = false)

  }

  implicit class SourceExt[T, M](val source: Source[T, M]) {

    def upTo(fun: T => Boolean): Source[T, M] = source via FlowUpTo(fun)

    def upToExcl(fun: T => Boolean): Source[T, M] = source via FlowUpTo(fun, inclusive = false)

  }

}

class MapPartial[Input, Output](fun: PartialFunction[Input, Output]) extends FlowStage[Input, Output]{
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    self=>

    setHandler(in, new InHandler {
      @throws[Exception](classOf[Exception])
      override def onPush(): Unit = {
        val element = self.grab(in)
        if(fun.isDefinedAt(element)) {
          val value = fun(element)
          push(out, value)
        } else self.completeStage()
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit ={
        pull(in)
      }
    })

  }
}

//inclusive flow
class UpToStage[T](fun: T => Boolean, inclusive: Boolean = true) extends FlowStage[T, T]{


  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    self=>

    setHandler(in, new InHandler {
      @throws[Exception](classOf[Exception])
      override def onPush(): Unit = {
        val element = self.grab(in)
        if(fun(element)) {
          if(inclusive) push(out, element)
          self.completeStage()
        } else push(out, element)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit ={
        pull(in)
      }
    })

  }

}

object FlowUpTo {
  def apply[T](fun: T => Boolean): Flow[T, T, NotUsed] = Flow.fromGraph(new UpToStage[T](fun))
  def apply[T, M](fun: T => Boolean, inclusive: Boolean): Flow[T, T, NotUsed] = Flow.fromGraph(new UpToStage[T](fun, inclusive))

}

trait FlowStage[In, Out] extends GraphStage[FlowShape[In, Out]]{

  val in = Inlet[In]("Input")

  val out= Outlet[Out]("Output")

  val shape = new FlowShape[In, Out](in, out)
}