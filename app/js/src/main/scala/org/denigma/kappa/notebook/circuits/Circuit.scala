package org.denigma.kappa.notebook.circuits

import org.denigma.binding.extensions._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

trait AutoActive {
  self: Circuit[_, _]=>
  activate()
}

/**
  * A class that can receive and send input/output messages to Websocket and to other components
  * @param input input messages Rx
  * @param output is used to send messages to the server
  */
abstract class Circuit[From, To](val input: Var[From], val output: Var[To]) {

  self =>

  protected def onInputMessage(message: From): Unit

  def activate(): Obs = {
    input.foreach(onInputMessage)
  }

  //TODO: choose better name
  def intoIncomingPort[T, From2 <: From](initialValue: T, skipFirst: Boolean = true)(fun: T => From2) = {
    val port = Var(initialValue)
    val value = port.map(fun)
    if(skipFirst) value.onChange(v=> input() = v) else value.foreach(v=> input() = v)
    port
  }

  //TODO: choose better name
  def intoOutoingPort[T, To2 <: To](initialValue: T, skipFirst: Boolean = true)(fun: T => To2) = {
    val port = Var(initialValue)
    val value = port.map(fun)
    if(skipFirst) value.onChange(v=> output() = v) else value.foreach(v=> output() = v)
    port
  }

  /**
    * Subscribes input to the Var
    * @param value Var
    * @param skipFirst if we send the first value to input or skip it
    * @tparam From2
    * @return
    */
  def incomingPort[From2 <: From](value: Var[From2], skipFirst: Boolean = true): Var[From2] = {
    if(skipFirst) value.onChange(v=> input() = v) else value.foreach(v=> input() =v)
    value
  }
  def incomingPortFrom[From2 <: From](value: From2, skipFirst: Boolean = true) = incomingPort(Var(value), skipFirst)

  /**
    * @param value Var
    * @param skipFirst
    * @tparam To2
    * @return
    */
  def outgoingPort[To2 <: To](value: Var[To2], skipFirst: Boolean = true): Var[To2] = {
    if(skipFirst) value.onChange(v=> output() = v) else value.foreach(v=> output() = v)
    value
  }

  def outgoingPortFrom[To2 <: To](value: To2, skipFirst: Boolean = true) = outgoingPort(Var(value), skipFirst)

}
