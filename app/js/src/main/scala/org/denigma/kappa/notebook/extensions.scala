package org.denigma.kappa.notebook

import rx._

import scala.Vector
import scala.collection.immutable._
import fastparse.all.Parsed
import fastparse.core.{Mutable, ParseCtx, Parser}


object extensions {

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

  implicit class RxSortedSet[T](source: Rx[SortedSet[T]])  {

    import org.denigma.binding.extensions._
    lazy val upd: Rx[SetUpdate[T]] = source.updates

    def updatesTo[U](vector: Var[Vector[U]])(convert: T => U)(implicit same: (T, U)=> Boolean): Var[Vector[U]] = {
      upd.onChange{
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
    def toSyncVector[U](convert: T => U)(implicit same: (T, U)=> Boolean): Var[Vector[U]] = updatesTo(Var(source.now.map(convert).toVector))(convert)(same)

  }

}
