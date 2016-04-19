package org.denigma.kappa.notebook

import rx._

import scala.Vector
import scala.collection.immutable._


object extensions {

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
