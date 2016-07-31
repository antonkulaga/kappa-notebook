package org.denigma.kappa.notebook

import rx._

import scala.collection.immutable._
import fastparse.all.Parsed
import fastparse.core.{Mutable, ParseCtx, Parser}
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import org.scalajs.dom.{Event, File, FileList, FileReader}
import org.scalajs.dom.ext.EasySeq
import org.scalajs.dom.raw.{Element, HTMLElement, ProgressEvent}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.ArrayBuffer

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
