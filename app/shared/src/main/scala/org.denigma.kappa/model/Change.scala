package org.denigma.kappa.model


object Change extends Enumeration {
  type Change = Value
  val Removed, Added, Unchanged, Updated = Value
}
