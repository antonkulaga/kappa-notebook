package org.denigma.kappa.notebook

import fastparse.all.Parsed

object extensions extends SharedExtensions{

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
