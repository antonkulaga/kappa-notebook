package org.denigma.kappa.notebook

import fastparse.all.Parsed
import fastparse.utils.{IndexedParserInput, IteratorParserInput}

object extensions extends SharedExtensions{

  implicit class ParsedExt[T](source: Parsed[T]) {

    def map[R](fun: T => R) = source match {
      case Parsed.Success(value, index) =>
        val result = fun(value)
        Parsed.Success.apply(result, index)
      case f:Parsed.Failure => f
    }

    def recover(recoverFun: String => Parsed[T]): Parsed[T] = source match {
      case f: Parsed.Failure =>
        f.extra.input match {
          case IndexedParserInput(data) => recoverFun(data)
          case IteratorParserInput(data) =>
            throw new Exception("Iterator recovery is not supported")
        }
      case other => other
    }


    def onSuccess(fun: T => Unit): Parsed[T] = source match {
      case s @ Parsed.Success(value, index) =>
        fun(s.value)
        s
      case f:Parsed.Failure => f
    }

    //TODO: I do not remember why I have unit here
    def onFailure(recover: String => Unit): Parsed[T] = source match {
      case s @ Parsed.Success(value, index) => s
      case f: Parsed.Failure =>
        f.extra.input match {
          case IndexedParserInput(data) =>
            recover(data)
            f
          case IteratorParserInput(data) =>
            throw new Exception("Iterator recovery is not supported")
        }
    }

  }
}
