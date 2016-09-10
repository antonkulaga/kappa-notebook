package org.denigma.kappa.notebook

import fastparse.all.Parsed
import org.scalajs.dom.ClientRect
import org.scalajs.dom.raw.Element

object extensions extends SharedExtensions{

  implicit class ClientRectExt(rect: ClientRect) {

    def intersects(other: ClientRect): Boolean = {
      rect.left <= other.right && rect.right >= other.left &&
      rect.top <= other.bottom && rect.bottom >= other.top
    }

  }

  implicit class BoundingExt(element: Element) {

    def intersects(other: Element): Boolean = {
      val rect = element.getBoundingClientRect()
      val otherRect = other.getBoundingClientRect()
      //println(s"RECT: left(${rect.left}) top(${rect.top}) right(${rect.right}) bottom(${rect.bottom})")
      //println(s"OTHER: left(${otherRect.left}) top(${otherRect.top}) right(${otherRect.right}) bottom(${otherRect.bottom})")
      rect.intersects(otherRect)
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
