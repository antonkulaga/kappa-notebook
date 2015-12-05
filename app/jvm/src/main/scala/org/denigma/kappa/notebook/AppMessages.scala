package org.denigma.kappa.notebook

import com.typesafe.config.Config

object AppMessages {

  case class Start(config:Config)
  case object Stop

}
