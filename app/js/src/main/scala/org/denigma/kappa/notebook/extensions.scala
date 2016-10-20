package org.denigma.kappa.notebook

import rx.{Rx, Var}

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js
import org.denigma.binding.extensions._

object extensions extends SharedExtensions{

  implicit def timers2[T](source: Rx[T]): TimerExtensions2[T] = new TimerExtensions2[T](source)

  class TimerExtensions2[T](val source: Rx[T]) extends AnyVal{

    def mapAfterLastChange[U](delay: FiniteDuration, initial: U)(fun: T => U): Var[U] = {
      val result = Var[U](initial)
      source.afterLastChange(delay){ value => result() = fun(value)}
      result
    }

    def mapAfterLastChange[U](delay: FiniteDuration)(fun: T => U): Var[U] = {
      mapAfterLastChange(delay, fun(source.now))(fun)
    }
  }

}
