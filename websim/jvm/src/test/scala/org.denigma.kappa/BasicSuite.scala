package org.denigma.kappa

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import com.typesafe.config.Config
import org.scalatest.concurrent.{Eventually, Futures}
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpec}

import scala.concurrent.duration._
/**
  * Created by antonkulaga on 31/03/16.
  */
class BasicSuite extends BasicSharedSuite with ScalatestRouteTest {

  implicit val timeout: Timeout = Timeout(duration)

  val config: Config = system.settings.config

  def log = system.log
}
class BasicKappaSuite extends BasicSuite with KappaRes