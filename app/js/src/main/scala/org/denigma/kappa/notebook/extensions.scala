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

  implicit class SetWatcher[T](col: Rx[Set[T]])
  {
    var previous = col.now
    val red: Rx[(Set[T], Set[T])] = Rx.unsafe{
      val old = previous
      previous = col() //TODO: maybe dangerous!
      (old, previous)
    }

    lazy val updates: Rx[SetUpdate[T]] = red map
      {
        case (prev, cur) => SetUpdate(prev.removeAddToBecome(cur))
      }

    def updatesTo[U](vector: Var[Vector[U]])(convert: T => U)(implicit same: (T, U)=> Boolean): Var[Vector[U]] = {
      updates.onChange{
        case u =>
          val remained: Vector[U] = vector.now.filterNot(v=>u.removed.exists(r=>same(r, v)))
          vector() = remained ++ u.added.map(convert)
      }
      vector
    }

    /**
      * Produces a vector that syncs with the set without applying convertions too many times
      *
      * @param convert
      * @param same
      * @tparam U
      * @return
      */
    def toSyncVector[U](convert: T => U)(implicit same: (T, U)=> Boolean): Var[Vector[U]] = updatesTo(Var(col.now.map(convert).toVector))(convert)(same)

  }

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

    def push(value: T) = {
      source.Internal.value = value
      source.propagate()
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

  implicit class ExtElement(el: HTMLElement) {
    def isHidden = el.offsetParent == null
    def isVisible = el.offsetParent !=null
  }

  implicit class FileListExt(files: FileList) extends EasySeq[File](files.length, files.item)

  implicit class FileOpt(f: org.scalajs.dom.File) {

    def readAsArrayBuffer: Future[ArrayBuffer] = {
      val result = Promise[ArrayBuffer]
      val reader = new FileReader()
      def onLoadEnd(ev: ProgressEvent): Any = {
        result.success(reader.result.asInstanceOf[ArrayBuffer])
      }
      def onErrorEnd(ev: Event): Any = {
        result.failure(new Exception("READING FAILURE " + ev.toString))
      }
      reader.onloadend = onLoadEnd _
      reader.onerror = onErrorEnd _
      reader.readAsArrayBuffer(f)
      result.future
    }

  }
}
