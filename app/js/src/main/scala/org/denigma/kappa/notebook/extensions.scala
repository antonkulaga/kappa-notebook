package org.denigma.kappa.notebook

import rx._

import scala.Vector
import scala.collection.immutable._
import fastparse.all.Parsed
import fastparse.core.{Mutable, ParseCtx, Parser}
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._

object extensions {

  implicit class AnyKappaRx[T](source: Rx[T]) {

    def triggerIf(value: Rx[Boolean])(fun: T => Unit) = {
      source.triggerLater(if (value.now) fun(source.now) )
    }
  }
  implicit class AnyKappaVar[T](source: Var[T]) {

    def extractVar[U](fromSource: T => U)(updateSource: (T, U) => T): Var[U] = { //no can be very dangerous
      val initial = fromSource(source.now)
      val variable = Var(initial)
      source.onChange{
        case s=>
          variable() = fromSource(s)
      }
      variable.onChange{
        case v =>
          source() = updateSource(source.now, v)
      }
      variable
    }
  }

  implicit class ParsedExt[T](source: Parsed[T]) {


    def map[R](fun: T => R): Parsed[R] = source match {
      case Parsed.Success(value, index) => Parsed.Success[R](fun(value),index)
      case f:Parsed.Failure => f
    }

    def recover(recoverFun: String => Parsed[T]): Parsed[T] = source match {
      case f: Parsed.Failure => recoverFun(f.extra.input)
      case other => other
    }


    def onSuccess(fun: T => Unit): Parsed[T] = source match {
      case s @ Parsed.Success(value, index) =>
        fun(s.value)
        s
      case f:Parsed.Failure => f
    }

    def onFailure(recover: String => Unit): Parsed[T] = source match {
      case s @ Parsed.Success(value, index) => s
      case f:Parsed.Failure =>
        recover(f.extra.input)
        f
    }


  }
}
