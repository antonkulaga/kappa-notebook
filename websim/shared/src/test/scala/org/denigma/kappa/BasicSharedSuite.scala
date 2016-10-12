package org.denigma.kappa

import org.scalatest.concurrent.{ Futures}
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpec}

import scala.concurrent.duration._

/**
  * Created by antonkulaga on 10/10/16.
  */
class BasicSharedSuite  extends WordSpec with Matchers with Futures with Inside with BeforeAndAfterAll {

  implicit val duration: FiniteDuration = 1 second

}
